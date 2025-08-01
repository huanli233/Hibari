/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huanli233.hibari.runtime.snapshots

import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterSetOf
import com.huanli233.hibari.runtime.checkPrecondition
import com.huanli233.hibari.runtime.collection.wrapIntoSet
import com.huanli233.hibari.runtime.internal.AtomicInt
import com.huanli233.hibari.runtime.internal.SnapshotThreadLocal
import com.huanli233.hibari.runtime.internal.currentThreadId
import com.huanli233.hibari.runtime.platform.SynchronizedObject
import com.huanli233.hibari.runtime.platform.makeSynchronizedObject
import com.huanli233.hibari.runtime.requirePrecondition
import com.huanli233.hibari.runtime.snapshots.tooling.creatingSnapshot
import com.huanli233.hibari.runtime.snapshots.tooling.dispatchObserverOnApplied
import com.huanli233.hibari.runtime.snapshots.tooling.dispatchObserverOnPreDispose
import com.huanli233.hibari.runtime.util.fastForEach
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A snapshot of the values return by mutable states and other state objects. All state object will
 * have the same value in the snapshot as they had when the snapshot was created unless they are
 * explicitly changed in the snapshot.
 *
 * To enter a snapshot call [enter]. The snapshot is the current snapshot as returned by
 * [currentSnapshot] until the control returns from the lambda (or until a nested [enter] is
 * called). All state objects will return the values associated with this snapshot, locally in the
 * thread, until [enter] returns. All other threads are unaffected.
 *
 * Snapshots can be nested by calling [takeNestedSnapshot].
 *
 * @see takeSnapshot
 * @see takeMutableSnapshot
 */
sealed class Snapshot(
    snapshotId: SnapshotId,

    /** A set of all the snapshots that should be treated as invalid. */
    internal open var invalid: SnapshotIdSet,
) {
    @Deprecated("Use id: Long constructor instead", level = DeprecationLevel.HIDDEN)
    protected constructor(id: Int, invalid: SnapshotIdSet) : this(id.toSnapshotId(), invalid)

    /**
     * The snapshot id of the snapshot. This is a unique number from a monotonically increasing
     * value for each snapshot taken.
     *
     * [id] will is identical to [snapshotId] if the value of [snapshotId] is less than or equal to
     * [Int.MAX_VALUE]. For [snapshotId] value greater than [Int.MAX_VALUE], this value will return
     * a negative value.
     */
    @Deprecated("Use snapshotId instead", replaceWith = ReplaceWith("snapshotId"))
    open val id: Int
        get() = snapshotId.toInt()

    /**
     * The snapshot id of the snapshot. This is a unique number from a monotonically increasing
     * value for each snapshot taken.
     */
    open var snapshotId: SnapshotId = snapshotId
        internal set

    internal open var writeCount: Int
        get() = 0
        @Suppress("UNUSED_PARAMETER")
        set(value) {
            error("Updating write count is not supported for this snapshot")
        }

    /**
     * The root snapshot for this snapshot. For non-nested snapshots this is always `this`. For
     * nested snapshot it is the parent's [root].
     */
    abstract val root: Snapshot

    /** True if any change to a state object in this snapshot will throw. */
    abstract val readOnly: Boolean

    /**
     * Dispose the snapshot. Neglecting to dispose a snapshot will result in difficult to diagnose
     * memory leaks as it indirectly causes all state objects to maintain its value for the
     * un-disposed snapshot.
     */
    open fun dispose() {
        disposed = true
        sync { releasePinnedSnapshotLocked() }
    }

    /**
     * Take a snapshot of the state values in this snapshot. The resulting [Snapshot] is read-only.
     * All nested snapshots need to be disposed by calling [dispose] before resources associated
     * with this snapshot can be collected. Nested snapshots are still valid after the parent has
     * been disposed.
     */
    abstract fun takeNestedSnapshot(readObserver: ((Any) -> Unit)? = null): Snapshot

    /**
     * Whether there are any pending changes in this snapshot. These changes are not visible until
     * the snapshot is applied.
     */
    abstract fun hasPendingChanges(): Boolean

    /**
     * Enter the snapshot. In [block] all state objects have the value associated with this
     * snapshot. The value of [currentSnapshot] will be this snapshot until this [block] returns or
     * a nested call to [enter] is called. When [block] returns, the previous current snapshot is
     * restored if there was one.
     *
     * All changes to state objects inside [block] are isolated to this snapshot and are not visible
     * to other snapshot or as global state. If this is a [readOnly] snapshot, any changes to state
     * objects will throw an [IllegalStateException].
     *
     * For a [MutableSnapshot], changes made to a snapshot inside [block] can be applied atomically
     * to the global state (or to its parent snapshot if it is a nested snapshot) by calling
     * [MutableSnapshot.apply].
     *
     * @see com.huanli233.hibari.runtime.mutableStateOf
     * @see com.huanli233.hibari.runtime.mutableStateListOf
     * @see com.huanli233.hibari.runtime.mutableStateMapOf
     */
    inline fun <T> enter(block: () -> T): T {
        val previous = makeCurrent()
        try {
            return block()
        } finally {
            restoreCurrent(previous)
        }
    }

    @PublishedApi
    internal open fun makeCurrent(): Snapshot? {
        val previous = threadSnapshot.get()
        threadSnapshot.set(this)
        return previous
    }

    @PublishedApi
    internal open fun restoreCurrent(snapshot: Snapshot?) {
        threadSnapshot.set(snapshot)
    }

    /**
     * Enter the snapshot, returning the previous [Snapshot] for leaving this snapshot later using
     * [unsafeLeave]. Prefer [enter] or [asContextElement] instead of using [unsafeEnter] directly
     * to prevent mismatched [unsafeEnter]/[unsafeLeave] calls.
     *
     * After returning all state objects have the value associated with this snapshot. The value of
     * [currentSnapshot] will be this snapshot until [unsafeLeave] is called with the returned
     * [Snapshot] or another call to [unsafeEnter] or [enter] is made.
     *
     * All changes to state objects until another snapshot is entered or this snapshot is left are
     * isolated to this snapshot and are not visible to other snapshot or as global state. If this
     * is a [readOnly] snapshot, any changes to state objects will throw an [IllegalStateException].
     *
     * For a [MutableSnapshot], changes made to a snapshot can be applied atomically to the global
     * state (or to its parent snapshot if it is a nested snapshot) by calling
     * [MutableSnapshot.apply].
     */
    fun unsafeEnter(): Snapshot? = makeCurrent()

    /** Leave the snapshot, restoring the [oldSnapshot] before returning. See [unsafeEnter]. */
    fun unsafeLeave(oldSnapshot: Snapshot?) {
        checkPrecondition(threadSnapshot.get() === this) {
            "Cannot leave snapshot; $this is not the current snapshot"
        }
        restoreCurrent(oldSnapshot)
    }

    internal var disposed = false

    /*
     * Handle to use when unpinning this snapshot. -1 if this snapshot has been unpinned.
     */
    @Suppress("LeakingThis")
    private var pinningTrackingHandle =
        if (snapshotId != INVALID_SNAPSHOT) trackPinning(snapshotId, invalid) else -1

    internal inline val isPinned
        get() = pinningTrackingHandle >= 0

    /*
     * The read observer for the snapshot if there is one.
     */
    @PublishedApi internal abstract val readObserver: ((Any) -> Unit)?

    /** The write observer for the snapshot if there is one. */
    internal abstract val writeObserver: ((Any) -> Unit)?

    /** Called when a nested snapshot of this snapshot is activated */
    internal abstract fun nestedActivated(snapshot: Snapshot)

    /** Called when a nested snapshot of this snapshot is deactivated */
    internal abstract fun nestedDeactivated(snapshot: Snapshot)

    /** Record that state was modified in the snapshot. */
    internal abstract fun recordModified(state: StateObject)

    /** The set of state objects that have been modified in this snapshot. */
    internal abstract val modified: MutableScatterSet<StateObject>?

    /**
     * Notify the snapshot that all objects created in this snapshot to this point should be
     * considered initialized. If any state object is modified after this point it will appear as
     * modified in the snapshot. Any applicable snapshot write observer will be called for the
     * object and the object will be part of the a set of mutated objects sent to any applicable
     * snapshot apply observer.
     *
     * Unless [notifyObjectsInitialized] is called, state objects created in a snapshot are not
     * considered modified by the snapshot even if they are modified after construction.
     */
    internal abstract fun notifyObjectsInitialized()

    /**
     * Closes the snapshot by removing the snapshot id (an any previous id's) from the list of open
     * snapshots and unpinning snapshots that no longer are referenced by this snapshot.
     */
    internal fun closeAndReleasePinning() {
        sync {
            closeLocked()
            releasePinnedSnapshotsForCloseLocked()
        }
    }

    /**
     * Closes the snapshot by removing the snapshot id (and any previous ids) from the list of open
     * snapshots. Does not release pinned snapshots. See [releasePinnedSnapshotsForCloseLocked] for
     * the second half of [closeAndReleasePinning].
     *
     * Call while holding a `sync {}` lock.
     */
    internal open fun closeLocked() {
        openSnapshots = openSnapshots.clear(snapshotId)
    }

    /**
     * Releases all pinned snapshots required to perform a clean [closeAndReleasePinning].
     *
     * Call while holding a `sync {}` lock.
     *
     * See [closeAndReleasePinning], [closeLocked].
     */
    internal open fun releasePinnedSnapshotsForCloseLocked() {
        releasePinnedSnapshotLocked()
    }

    internal fun validateNotDisposed() {
        requirePrecondition(!disposed) { "Cannot use a disposed snapshot" }
    }

    internal fun releasePinnedSnapshotLocked() {
        if (pinningTrackingHandle >= 0) {
            releasePinningLocked(pinningTrackingHandle)
            pinningTrackingHandle = -1
        }
    }

    internal fun takeoverPinnedSnapshot(): Int =
        pinningTrackingHandle.also { pinningTrackingHandle = -1 }

    companion object {
        /**
         * Return the thread's active snapshot. If no thread snapshot is active then the current
         * global snapshot is used.
         */
        val current: Snapshot
            get() = currentSnapshot()

        /** Return `true` if the thread is currently in the context of a snapshot. */
        val isInSnapshot: Boolean
            get() = threadSnapshot.get() != null

        /**
         * Returns whether any threads are currently in the process of notifying observers about
         * changes to the global snapshot.
         */
        val isApplyObserverNotificationPending: Boolean
            get() = pendingApplyObserverCount.get() > 0

        /**
         * All new state objects initial state records should be [PreexistingSnapshotId] which then
         * allows snapshots outside the creating snapshot to access the object with its initial
         * state.
         */
        @Suppress("ConstPropertyName")
        const val PreexistingSnapshotId: Int = 1

        /**
         * Take a snapshot of the current value of all state objects. The values are preserved until
         * [Snapshot.dispose] is called on the result.
         *
         * The [readObserver] parameter can be used to track when all state objects are read when in
         * [Snapshot.enter]. A snapshot apply observer can be registered using
         * [Snapshot.registerApplyObserver] to observe modification of state objects.
         *
         * An active snapshot (after it is created but before [Snapshot.dispose] is called) requires
         * resources to track the values in the snapshot. Once a snapshot is no longer needed it
         * should disposed by calling [Snapshot.dispose].
         *
         * Leaving a snapshot active could cause hard to diagnose memory leaks values as are
         * maintained by state objects for these unneeded snapshots. Take care to always call
         * [Snapshot.dispose] on all snapshots when they are no longer needed.
         *
         * Composition uses both of these to implicitly subscribe to changes to state object and
         * automatically update the composition when state objects read during composition change.
         *
         * A nested snapshot can be taken of a snapshot which is an independent read-only copy of
         * the snapshot and can be disposed independently. This is used by [takeSnapshot] when in a
         * read-only snapshot for API consistency allowing the result of [takeSnapshot] to be
         * disposed leaving the parent snapshot active.
         *
         * @param readObserver called when any state object is read in the lambda passed to
         *   [Snapshot.enter] or in the [Snapshot.enter] of any nested snapshot.
         * @see Snapshot
         * @see Snapshot.registerApplyObserver
         */
        fun takeSnapshot(readObserver: ((Any) -> Unit)? = null): Snapshot =
            currentSnapshot().takeNestedSnapshot(readObserver)

        /**
         * Take a snapshot of the current value of all state objects that also allows the state to
         * be changed and later atomically applied when [MutableSnapshot.apply] is called. The
         * values are preserved until [Snapshot.dispose] is called on the result. The global state
         * will either see all the changes made as one atomic change, when [MutableSnapshot .apply]
         * is called, or none of the changes if the mutable state object is disposed before being
         * applied.
         *
         * The values in a snapshot can be modified by calling [Snapshot.enter] and then, in its
         * lambda, modify any state object. The new values of the state objects will only become
         * visible to the global state when [MutableSnapshot.apply] is called.
         *
         * An active snapshot (after it is created but before [Snapshot.dispose] is called) requires
         * resources to track the values in the snapshot. Once a snapshot is no longer needed it
         * should disposed by calling [Snapshot.dispose].
         *
         * Leaving a snapshot active could cause hard to diagnose memory leaks as values are
         * maintained by state objects for these unneeded snapshots. Take care to always call
         * [Snapshot.dispose] on all snapshots when they are no longer needed.
         *
         * A nested snapshot can be taken by calling [Snapshot.takeNestedSnapshot], for a read-only
         * snapshot, or [MutableSnapshot.takeNestedMutableSnapshot] for a snapshot that can be
         * changed. Nested mutable snapshots are applied to the this, the parent snapshot, when
         * their [MutableSnapshot.apply] is called. Their applied changes will be visible to in this
         * snapshot but will not be visible other snapshots (including other nested snapshots) or
         * the global state until this snapshot is applied by calling [MutableSnapshot.apply].
         *
         * Once [MutableSnapshot.apply] is called on this, the parent snapshot, all calls to
         * [MutableSnapshot.apply] on an active nested snapshot will fail.
         *
         * Changes to a mutable snapshot are isolated, using snapshot isolation, from all other
         * snapshots. Their changes are only visible as global state or to new snapshots once
         * [MutableSnapshot.apply] is called.
         *
         * Applying a snapshot can fail if currently visible changes to the state object conflicts
         * with a change made in the snapshot.
         *
         * When in a mutable snapshot, [takeMutableSnapshot] creates a nested snapshot of the
         * current mutable snapshot. If the current snapshot is read-only, an exception is thrown.
         * The current snapshot is the result of calling [currentSnapshot] which is updated by
         * calling [Snapshot.enter] which makes the [Snapshot] the current snapshot while in its
         * lambda.
         *
         * Composition uses mutable snapshots to allow changes made in a [Composable] functions to
         * be temporarily isolated from the global state and is later applied to the global state
         * when the composition is applied. If [MutableSnapshot.apply] fails applying this snapshot,
         * the snapshot and the changes calculated during composition are disposed and a new
         * composition is scheduled to be calculated again.
         *
         * @param readObserver called when any state object is read in the lambda passed to
         *   [Snapshot.enter] or in the [Snapshot.enter] of any nested snapshots.
         *
         * Composition, layout and draw use [readObserver] to implicitly subscribe to changes to
         * state objects to know when to update.
         *
         * @param writeObserver called when a state object is created or just before it is written
         *   to the first time in the snapshot or a nested mutable snapshot. This might be called
         *   several times for the same object if nested mutable snapshots are created.
         *
         * Composition uses [writeObserver] to track when a state object is modified during
         * composition in order to invalidate the reads that have not yet occurred. This allows a
         * single pass of composition for state objects that are written to before they are read
         * (such as modifying the value of a dynamic ambient provider).
         *
         * @see Snapshot.takeSnapshot
         * @see Snapshot
         * @see MutableSnapshot
         */
        fun takeMutableSnapshot(
            readObserver: ((Any) -> Unit)? = null,
            writeObserver: ((Any) -> Unit)? = null,
        ): MutableSnapshot =
            (currentSnapshot() as? MutableSnapshot)?.takeNestedMutableSnapshot(
                readObserver,
                writeObserver,
            ) ?: error("Cannot create a mutable snapshot of an read-only snapshot")

        /**
         * Escape the current snapshot, if there is one. All state objects will have the value
         * associated with the global while the [block] lambda is executing.
         *
         * @return the result of [block]
         */
        inline fun <T> global(block: () -> T): T {
            val previous = removeCurrent()
            try {
                return block()
            } finally {
                restoreCurrent(previous)
            }
        }

        /**
         * Take a [MutableSnapshot] and run [block] within it. When [block] returns successfully,
         * attempt to [MutableSnapshot.apply] the snapshot. Returns the result of [block] or throws
         * [SnapshotApplyConflictException] if snapshot changes attempted by [block] could not be
         * applied.
         *
         * Prior to returning, any changes made to snapshot state (e.g. state holders returned by
         * [com.huanli233.hibari.runtime.mutableStateOf] are not visible to other threads. When
         * [withMutableSnapshot] returns successfully those changes will be made visible to other
         * threads and any snapshot observers (e.g. [androidx.compose.runtime.snapshotFlow]) will be
         * notified of changes.
         *
         * [block] must not suspend if [withMutableSnapshot] is called from a suspend function.
         */
        // TODO: determine a good way to prevent/discourage suspending in an inlined [block]
        inline fun <R> withMutableSnapshot(block: () -> R): R =
            takeMutableSnapshot().run {
                var hasError = false
                try {
                    enter(block)
                } catch (e: Throwable) {
                    hasError = true
                    throw e
                } finally {
                    if (!hasError) {
                        apply().check()
                    }
                    dispose()
                }
            }

        /**
         * Observe reads and or write of state objects in the current thread.
         *
         * This only affects the current snapshot (if any) and any new snapshots create from
         * [Snapshot.takeSnapshot] and [takeMutableSnapshot]. It will not affect any snapshots
         * previous created even if [Snapshot.enter] is called in [block].
         *
         * @param readObserver called when any state object is read.
         * @param writeObserver called when a state object is created or just before it is written
         *   to the first time in the snapshot or a nested mutable snapshot. This might be called
         *   several times for the same object if nested mutable snapshots are created.
         * @param block the code the [readObserver] and [writeObserver] will be observing. Once
         *   [block] returns, the [readObserver] and [writeObserver] will no longer be called.
         */
        fun <T> observe(
            readObserver: ((Any) -> Unit)? = null,
            writeObserver: ((Any) -> Unit)? = null,
            block: () -> T,
        ): T {
            if (readObserver == null && writeObserver == null) {
                // No observer change, just execute the block
                return block()
            }

            val previous = threadSnapshot.get()
            if (previous is TransparentObserverMutableSnapshot && previous.canBeReused) {
                // Change observers in place without allocating new snapshots.
                val previousReadObserver = previous.readObserver
                val previousWriteObserver = previous.writeObserver

                try {
                    previous.readObserver = mergedReadObserver(readObserver, previousReadObserver)
                    previous.writeObserver =
                        mergedWriteObserver(writeObserver, previousWriteObserver)
                    return block()
                } finally {
                    previous.readObserver = previousReadObserver
                    previous.writeObserver = previousWriteObserver
                }
            } else {
                // The snapshot is not already transparent, observe in a new transparent snapshot
                val snapshot =
                    when {
                        previous == null || previous is MutableSnapshot -> {
                            TransparentObserverMutableSnapshot(
                                parentSnapshot = previous as? MutableSnapshot,
                                specifiedReadObserver = readObserver,
                                specifiedWriteObserver = writeObserver,
                                mergeParentObservers = true,
                                ownsParentSnapshot = false,
                            )
                        }
                        readObserver == null -> {
                            return block()
                        }
                        else -> {
                            previous.takeNestedSnapshot(readObserver)
                        }
                    }
                try {
                    return snapshot.enter(block)
                } finally {
                    snapshot.dispose()
                }
            }
        }

        @Suppress("unused") // left here for binary compatibility
        @PublishedApi
        internal fun createNonObservableSnapshot(): Snapshot =
            createTransparentSnapshotWithNoParentReadObserver(
                previousSnapshot = threadSnapshot.get()
            )

        @PublishedApi
        internal val currentThreadSnapshot: Snapshot?
            get() = threadSnapshot.get()

        private inline val TransparentObserverMutableSnapshot.canBeReused: Boolean
            get() = threadId == currentThreadId()

        private inline val TransparentObserverSnapshot.canBeReused: Boolean
            get() = threadId == currentThreadId()

        @PublishedApi
        internal fun makeCurrentNonObservable(previous: Snapshot?): Snapshot =
            when {
                previous is TransparentObserverMutableSnapshot && previous.canBeReused -> {
                    previous.readObserver = null
                    previous
                }
                previous is TransparentObserverSnapshot && previous.canBeReused -> {
                    previous.readObserver = null
                    previous
                }
                else -> {
                    val snapshot =
                        createTransparentSnapshotWithNoParentReadObserver(
                            previousSnapshot = previous
                        )
                    snapshot.makeCurrent()
                    snapshot
                }
            }

        @PublishedApi
        internal fun restoreNonObservable(
            previous: Snapshot?,
            nonObservable: Snapshot,
            observer: ((Any) -> Unit)?,
        ) {
            if (previous === nonObservable) {
                when (previous) {
                    is TransparentObserverMutableSnapshot -> {
                        previous.readObserver = observer
                    }
                    is TransparentObserverSnapshot -> {
                        previous.readObserver = observer
                    }
                    else -> {
                        error("Non-transparent snapshot was reused: $previous")
                    }
                }
            } else {
                nonObservable.restoreCurrent(previous)
                nonObservable.dispose()
            }
        }

        /**
         * Passed [block] will be run with all the currently set snapshot read observers disabled.
         */
        @Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
        @OptIn(ExperimentalContracts::class)
        inline fun <T> withoutReadObservation(block: () -> T): T {
            contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
            val previousSnapshot = currentThreadSnapshot
            val observer = previousSnapshot?.readObserver
            val newSnapshot = makeCurrentNonObservable(previousSnapshot)
            try {
                return block()
            } finally {
                restoreNonObservable(previousSnapshot, newSnapshot, observer)
            }
        }

        /**
         * Register an apply listener that is called back when snapshots are applied to the global
         * state.
         *
         * @return [ObserverHandle] to unregister [observer].
         */
        fun registerApplyObserver(observer: (Set<Any>, Snapshot) -> Unit): ObserverHandle {
            // Ensure observer does not see changes before this call.
            advanceGlobalSnapshot(emptyLambda)

            sync { applyObservers += observer }
            return ObserverHandle { sync { applyObservers -= observer } }
        }

        /**
         * Register an observer of the first write to the global state of a global state object
         * since the last call to [sendApplyNotifications].
         *
         * Composition uses this to schedule a new composition whenever a state object that was read
         * in composition is modified.
         *
         * State objects can be sent to the apply observer that have not been sent to global write
         * observers. This happens for state objects inside [MutableSnapshot] that is later applied
         * by calling [MutableSnapshot.apply].
         *
         * This should only be used to determine if a call to [sendApplyNotifications] should be
         * scheduled to be called.
         *
         * @return [ObserverHandle] to unregister [observer].
         */
        fun registerGlobalWriteObserver(observer: ((Any) -> Unit)): ObserverHandle {
            sync { globalWriteObservers += observer }
            advanceGlobalSnapshot()
            return ObserverHandle {
                sync { globalWriteObservers -= observer }
                advanceGlobalSnapshot()
            }
        }

        /**
         * Notify the snapshot that all objects created in this snapshot to this point should be
         * considered initialized. If any state object is are modified passed this point it will
         * appear as modified in the snapshot and any applicable snapshot write observer will be
         * called for the object and the object will be part of the a set of mutated objects sent to
         * any applicable snapshot apply observer.
         *
         * Unless [notifyObjectsInitialized] is called, state objects created in a snapshot are not
         * considered modified by the snapshot even if they are modified after construction.
         *
         * Compose uses this between phases of composition to allow observing changes to state
         * objects create in a previous phase.
         */
        fun notifyObjectsInitialized(): Unit = currentSnapshot().notifyObjectsInitialized()

        /**
         * Send any pending apply notifications for state objects changed outside a snapshot.
         *
         * Apply notifications for state objects modified outside snapshot are deferred until method
         * is called. This method is implicitly called whenever a non-nested [MutableSnapshot] is
         * applied making its changes visible to all new, non-nested snapshots.
         *
         * Composition schedules this to be called after changes to state objects are detected an
         * observer registered with [registerGlobalWriteObserver].
         */
        fun sendApplyNotifications() {
            val changes = sync { globalSnapshot.hasPendingChanges() }
            if (changes) advanceGlobalSnapshot()
        }

        fun openSnapshotCount(): Int = openSnapshots.toList().size

        @PublishedApi
        internal fun removeCurrent(): Snapshot? {
            val previous = threadSnapshot.get()
            if (previous != null) threadSnapshot.set(null)
            return previous
        }

        @PublishedApi
        internal fun restoreCurrent(previous: Snapshot?) {
            if (previous != null) threadSnapshot.set(previous)
        }
    }
}

/**
 * Pin the snapshot and invalid set.
 *
 * @return returns a handle that should be passed to [releasePinningLocked] when the snapshot closes
 *   or is disposed.
 */
internal fun trackPinning(snapshotId: SnapshotId, invalid: SnapshotIdSet): Int {
    val pinned = invalid.lowest(snapshotId)
    return sync { pinningTable.add(pinned) }
}

/** Release the [handle] returned by [trackPinning] */
internal fun releasePinningLocked(handle: Int) {
    pinningTable.remove(handle)
}

/**
 * A snapshot of the values return by mutable states and other state objects. All state object will
 * have the same value in the snapshot as they had when the snapshot was created unless they are
 * explicitly changed in the snapshot.
 *
 * To enter a snapshot call [enter]. The snapshot is the current snapshot as returned by
 * [currentSnapshot] until the control returns from the lambda (or until a nested [enter] is called.
 * All state objects will return the values associated with this snapshot, locally in the thread,
 * until [enter] returns. All other threads are unaffected.
 *
 * All changes made in a [MutableSnapshot] are snapshot isolated from all other snapshots and their
 * changes can only be seen globally, or by new shots, after [MutableSnapshot.apply] as been called.
 *
 * Snapshots can be nested by calling [takeNestedSnapshot] or
 * [MutableSnapshot.takeNestedMutableSnapshot].
 *
 * @see Snapshot.takeMutableSnapshot
 * @see com.huanli233.hibari.runtime.mutableStateOf
 * @see com.huanli233.hibari.runtime.mutableStateListOf
 * @see com.huanli233.hibari.runtime.mutableStateMapOf
 */
open class MutableSnapshot
internal constructor(
    snapshotId: SnapshotId,
    invalid: SnapshotIdSet,
    override val readObserver: ((Any) -> Unit)?,
    override val writeObserver: ((Any) -> Unit)?,
) : Snapshot(snapshotId, invalid) {
    /**
     * Whether there are any pending changes in this snapshot. These changes are not visible until
     * the snapshot is applied.
     */
    override fun hasPendingChanges(): Boolean = modified?.isNotEmpty() == true

    /**
     * Take a mutable snapshot of the state values in this snapshot. Entering this snapshot by
     * calling [enter] allows state objects to be modified that are not visible to the this, the
     * parent snapshot, until the [apply] is called.
     *
     * Applying a nested snapshot, by calling [apply], applies its change to, this, the parent
     * snapshot. For a change to be visible globally, all the parent snapshots need to be applied
     * until the root snapshot is applied to the global state.
     *
     * All nested snapshots need to be disposed by calling [dispose] before resources associated
     * with this snapshot can be collected. Nested active snapshots are still valid after the parent
     * has been disposed but calling [apply] will fail.
     */
    open fun takeNestedMutableSnapshot(
        readObserver: ((Any) -> Unit)? = null,
        writeObserver: ((Any) -> Unit)? = null,
    ): MutableSnapshot {
        validateNotDisposed()
        validateNotAppliedOrPinned()
        return creatingSnapshot(this, readObserver, writeObserver, readonly = false) {
                actualReadObserver,
                actualWriteObserver ->
            advance {
                sync {
                    val newId = nextSnapshotId
                    nextSnapshotId += 1
                    openSnapshots = openSnapshots.set(newId)
                    val currentInvalid = invalid
                    this.invalid = currentInvalid.set(newId)
                    NestedMutableSnapshot(
                        newId,
                        currentInvalid.addRange(snapshotId + 1, newId),
                        mergedReadObserver(actualReadObserver, this.readObserver),
                        mergedWriteObserver(actualWriteObserver, this.writeObserver),
                        this,
                    )
                }
            }
        }
    }

    /**
     * Apply the changes made to state objects in this snapshot to the global state, or to the
     * parent snapshot if this is a nested mutable snapshot.
     *
     * Once this method returns all changes made to this snapshot are atomically visible as the
     * global state of the state object or to the parent snapshot.
     *
     * While a snapshot is active (after it is created but before [apply] or [dispose] is called)
     * requires resources to track the values in the snapshot. Once a snapshot is no longer needed
     * it should be either applied by calling [apply] or disposed by calling [dispose]. A snapshot
     * that has been had is [apply] called can also have [dispose] called on it. However, calling
     * [apply] after calling [dispose] will throw an exception.
     *
     * Leaving a snapshot active could cause hard to diagnose memory leaks values are maintained by
     * state objects for unneeded snapshots. Take care to always call [dispose] on any snapshot.
     */
    open fun apply(): SnapshotApplyResult {
        // NOTE: the this algorithm is currently does not guarantee serializable snapshots as it
        // doesn't prevent crossing writes as described here https://arxiv.org/pdf/1412.2324.pdf

        // Just removing the snapshot from the active snapshot set is enough to make it part of the
        // next snapshot, however, this should only be done after first determining that there are
        // no
        // colliding writes are being applied.

        // A write is considered colliding if any write occurred in a state object in a snapshot
        // applied since the snapshot was taken.
        val modified = modified
        val optimisticMerges =
            if (modified != null) {
                val globalSnapshot = globalSnapshot
                optimisticMerges(
                    globalSnapshot.snapshotId,
                    this,
                    openSnapshots.clear(globalSnapshot.snapshotId),
                )
            } else null

        var observers = emptyList<(Set<Any>, Snapshot) -> Unit>()
        var globalModified: MutableScatterSet<StateObject>? = null
        sync {
            validateOpen(this)
            if (modified == null || modified.size == 0) {
                closeLocked()
                val globalSnapshot = globalSnapshot
                val previousModified = globalSnapshot.modified
                resetGlobalSnapshotLocked(globalSnapshot, emptyLambda)
                if (previousModified != null && previousModified.isNotEmpty()) {
                    observers = applyObservers
                    globalModified = previousModified
                }
            } else {
                val globalSnapshot = globalSnapshot
                val result =
                    innerApplyLocked(
                        nextSnapshotId,
                        modified,
                        optimisticMerges,
                        openSnapshots.clear(globalSnapshot.snapshotId),
                    )
                if (result != SnapshotApplyResult.Success) return result

                closeLocked()

                // Take a new global snapshot that includes this one.
                val previousModified = globalSnapshot.modified
                resetGlobalSnapshotLocked(globalSnapshot, emptyLambda)
                this.modified = null
                globalSnapshot.modified = null

                observers = applyObservers
                globalModified = previousModified
            }
        }

        // Mark as applied
        applied = true

        // Notify any apply observers that changes applied were seen
        if (globalModified != null) {
            val nonNullGlobalModified = globalModified!!.wrapIntoSet()
            if (nonNullGlobalModified.isNotEmpty()) {
                observers.fastForEach { it(nonNullGlobalModified, this) }
            }
        }

        if (modified != null && modified.isNotEmpty()) {
            val modifiedSet = modified.wrapIntoSet()
            observers.fastForEach { it(modifiedSet, this) }
        }

        dispatchObserverOnApplied(this, modified)

        // Wait to release pinned snapshots until after running observers.
        // This permits observers to safely take a nested snapshot of the one that was just applied
        // before unpinning records that need to be retained in this case.
        sync {
            releasePinnedSnapshotsForCloseLocked()
            checkAndOverwriteUnusedRecordsLocked()
            globalModified?.forEach { processForUnusedRecordsLocked(it) }
            modified?.forEach { processForUnusedRecordsLocked(it) }
            merged?.fastForEach { processForUnusedRecordsLocked(it) }
            merged = null
        }

        return SnapshotApplyResult.Success
    }

    override val readOnly: Boolean
        get() = false

    override val root: Snapshot
        get() = this

    override fun dispose() {
        if (!disposed) {
            super.dispose()
            nestedDeactivated(this)
            dispatchObserverOnPreDispose(this)
        }
    }

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        validateNotDisposed()
        validateNotAppliedOrPinned()
        val previousId = snapshotId
        return creatingSnapshot(
            if (this is GlobalSnapshot) null else this,
            readObserver = readObserver,
            writeObserver = null,
            readonly = true,
        ) { actualReadObserver, _ ->
            advance {
                sync {
                    val readonlyId = nextSnapshotId.also { nextSnapshotId += 1 }
                    openSnapshots = openSnapshots.set(readonlyId)
                    NestedReadonlySnapshot(
                        snapshotId = readonlyId,
                        invalid = invalid.addRange(previousId + 1, readonlyId),
                        readObserver = mergedReadObserver(actualReadObserver, this.readObserver),
                        parent = this,
                    )
                }
            }
        }
    }

    override fun nestedActivated(snapshot: Snapshot) {
        snapshots++
    }

    override fun nestedDeactivated(snapshot: Snapshot) {
        requirePrecondition(snapshots > 0) { "no pending nested snapshots" }
        if (--snapshots == 0) {
            if (!applied) {
                abandon()
            }
        }
    }

    override fun notifyObjectsInitialized() {
        if (applied || disposed) return
        advance()
    }

    override fun closeLocked() {
        // Remove itself and previous ids from the open set.
        openSnapshots = openSnapshots.clear(snapshotId).andNot(previousIds)
    }

    override fun releasePinnedSnapshotsForCloseLocked() {
        releasePreviouslyPinnedSnapshotsLocked()
        super.releasePinnedSnapshotsForCloseLocked()
    }

    private fun validateNotApplied() {
        checkPrecondition(!applied) { "Unsupported operation on a snapshot that has been applied" }
    }

    private fun validateNotAppliedOrPinned() {
        checkPrecondition(!applied || isPinned) {
            "Unsupported operation on a disposed or applied snapshot"
        }
    }

    /**
     * Abandon the snapshot. This does NOT [closeAndReleasePinning], which must be done as an
     * additional step by callers.
     */
    private fun abandon() {
        val modified = modified
        if (modified != null) {
            validateNotApplied()

            // Mark all state records created in this snapshot as invalid. This allows the snapshot
            // id to be forgotten as no state records will refer to it.
            this.modified = null
            val id = snapshotId
            modified.forEach { state ->
                var current: StateRecord? = state.firstStateRecord
                while (current != null) {
                    if (current.snapshotId == id || current.snapshotId in previousIds) {
                        current.snapshotId = INVALID_SNAPSHOT
                    }
                    current = current.next
                }
            }
        }

        // The snapshot can now be closed.
        closeAndReleasePinning()
    }

    internal fun innerApplyLocked(
        nextId: SnapshotId,
        modified: MutableScatterSet<StateObject>,
        optimisticMerges: Map<StateRecord, StateRecord>?,
        invalidSnapshots: SnapshotIdSet,
    ): SnapshotApplyResult {
        // This must be called in a synchronized block

        // If there are modifications we need to ensure none of the modifications have
        // collisions.

        // A record is guaranteed not collide if no other write was performed to the record
        // by an applied snapshot since this snapshot was taken. No writes to a state object
        // occurred if, ignoring this snapshot, the readable records for the snapshots are
        // the same. If they are different then there is a potential collision and the state
        // object is asked if it can resolve the collision. If it can the updated state record
        // is for the apply.
        var mergedRecords: MutableList<Pair<StateObject, StateRecord>>? = null
        val start = this.invalid.set(this.snapshotId).or(this.previousIds)
        var statesToRemove: MutableList<StateObject>? = null
        modified.forEach { state ->
            val first = state.firstStateRecord
            // If either current or previous cannot be calculated the object was created
            // in a nested snapshot that was committed then changed.
            val current = readable(first, nextId, invalidSnapshots) ?: return@forEach
            val previous = readable(first, this.snapshotId, start) ?: return@forEach
            if (previous.snapshotId == PreexistingSnapshotId.toSnapshotId()) {
                // A previous record might not be found if the state object was created in a
                // nested snapshot that didn't have any other modifications. The `apply()` for
                // a nested snapshot considers such snapshots no-op snapshots and just closes them
                // which allows this object's previous record to be missing or be the record created
                // during initial construction. In these cases taking applied is the right choice
                // this indicates there was no conflicting writes.
                return@forEach
            }
            if (current != previous) {
                val applied = readable(first, this.snapshotId, this.invalid) ?: readError()
                val merged =
                    optimisticMerges?.get(current)
                        ?: run { state.mergeRecords(previous, current, applied) }
                when (merged) {
                    null -> return SnapshotApplyResult.Failure(this)
                    applied -> {
                        // Nothing to do the merge policy says that the current changes
                        // obscure the current value so ignore the conflict
                    }
                    current -> {
                        (mergedRecords
                            ?: mutableListOf<Pair<StateObject, StateRecord>>().also {
                                mergedRecords = it
                            })
                            .add(state to current.create(snapshotId))

                        // If we revert to current then the state is no longer modified.
                        (statesToRemove
                            ?: mutableListOf<StateObject>().also { statesToRemove = it })
                            .add(state)
                    }
                    else -> {
                        (mergedRecords
                            ?: mutableListOf<Pair<StateObject, StateRecord>>().also {
                                mergedRecords = it
                            })
                            .add(
                                if (merged != previous) state to merged
                                else state to previous.create(snapshotId)
                            )
                    }
                }
            }
        }

        mergedRecords?.let {
            // Ensure we have a new snapshot id
            advance()

            // Update all the merged records to have the new id.
            it.fastForEach { merged ->
                val (state, stateRecord) = merged
                stateRecord.snapshotId = nextId
                sync {
                    stateRecord.next = state.firstStateRecord
                    state.prependStateRecord(stateRecord)
                }
            }
        }

        statesToRemove?.let { list ->
            list.fastForEach { modified.remove(it) }
            val mergedList = merged
            merged = if (mergedList == null) list else mergedList + list
        }

        return SnapshotApplyResult.Success
    }

    internal inline fun <T> advance(block: () -> T): T {
        recordPrevious(snapshotId)
        return block().also {
            // Only advance this snapshot if it's possible for it to be applied later,
            // otherwise we don't need to bother.
            // This simplifies tracking of open snapshots when an apply observer takes
            // a nested snapshot of the snapshot that was just applied.
            if (!applied && !disposed) {
                val previousId = snapshotId
                sync {
                    snapshotId = nextSnapshotId.also { nextSnapshotId += 1 }
                    openSnapshots = openSnapshots.set(snapshotId)
                }
                invalid = invalid.addRange(previousId + 1, snapshotId)
            }
        }
    }

    internal fun advance(): Unit = advance {}

    internal fun recordPrevious(id: SnapshotId) {
        sync { previousIds = previousIds.set(id) }
    }

    internal fun recordPreviousPinnedSnapshot(id: Int) {
        if (id >= 0) previousPinnedSnapshots += id
    }

    internal fun recordPreviousPinnedSnapshots(handles: IntArray) {
        // Avoid unnecessary copies implied by the `+` below.
        if (handles.isEmpty()) return
        val pinned = previousPinnedSnapshots
        previousPinnedSnapshots = if (pinned.isEmpty()) handles else pinned + handles
    }

    private fun releasePreviouslyPinnedSnapshotsLocked() {
        for (index in previousPinnedSnapshots.indices) {
            releasePinningLocked(previousPinnedSnapshots[index])
        }
    }

    internal fun recordPreviousList(snapshots: SnapshotIdSet) {
        sync { previousIds = previousIds.or(snapshots) }
    }

    override fun recordModified(state: StateObject) {
        (modified ?: mutableScatterSetOf<StateObject>().also { modified = it }).add(state)
    }

    override var writeCount: Int = 0

    override var modified: MutableScatterSet<StateObject>? = null

    internal var merged: List<StateObject>? = null

    /**
     * A set of the id's previously associated with this snapshot. When this snapshot closes then
     * these ids must be removed from the global as well.
     */
    internal var previousIds: SnapshotIdSet = SnapshotIdSet.EMPTY

    /** A list of the pinned snapshots handles that must be released by this snapshot */
    internal var previousPinnedSnapshots: IntArray = EmptyIntArray

    /**
     * The number of pending nested snapshots of this snapshot. To simplify the code, this snapshot
     * it, itself, counted as its own nested snapshot.
     */
    private var snapshots = 1

    /** Tracks whether the snapshot has been applied. */
    internal var applied = false

    private companion object {
        private val EmptyIntArray = IntArray(0)
    }
}

/**
 * The result of a applying a mutable snapshot. [Success] indicates that the snapshot was
 * successfully applied and is now visible as the global state of the state object (or visible in
 * the parent snapshot for a nested snapshot). [Failure] indicates one or more state objects were
 * modified by both this snapshot and in the global (or parent) snapshot, and the changes from this
 * snapshot are **not** visible in the global or parent snapshot.
 */
sealed class SnapshotApplyResult {
    /**
     * Check the result of an apply. If the result is [Success] then this does does nothing. If the
     * result is [Failure] then a [SnapshotApplyConflictException] exception is thrown. Once [check]
     * as been called the snapshot is disposed.
     */
    abstract fun check()

    /** True if the result is [Success]. */
    abstract val succeeded: Boolean

    object Success : SnapshotApplyResult() {
        /**
         * Check the result of a snapshot apply. Calling [check] on a [Success] result is a noop.
         */
        override fun check() {}

        override val succeeded: Boolean
            get() = true
    }

    class Failure(val snapshot: Snapshot) : SnapshotApplyResult() {
        /**
         * Check the result of a snapshot apply. Calling [check] on a [Failure] result throws a
         * [SnapshotApplyConflictException] exception.
         */
        override fun check() {
            snapshot.dispose()
            throw SnapshotApplyConflictException(snapshot)
        }

        override val succeeded: Boolean
            get() = false
    }
}

/**
 * The type returned by observer registration methods that unregisters the observer when it is
 * disposed.
 */
@Suppress("CallbackName")
fun interface ObserverHandle {
    /** Dispose the observer causing it to be unregistered from the snapshot system. */
    fun dispose()
}

/**
 * Return the thread's active snapshot. If no thread snapshot is active then the current global
 * snapshot is used.
 */
internal fun currentSnapshot(): Snapshot = threadSnapshot.get() ?: globalSnapshot

/**
 * An exception that is thrown when [SnapshotApplyResult.check] is called on a result of a
 * [MutableSnapshot.apply] that fails to apply.
 */
class SnapshotApplyConflictException(@Suppress("unused") val snapshot: Snapshot) :
    Exception()

/** Snapshot local value of a state object. */
abstract class StateRecord(
    /** The snapshot id of the snapshot in which the record was created. */
    internal var snapshotId: SnapshotId
) {
    constructor() : this(currentSnapshot().snapshotId)

    @Deprecated("Use snapshotId: Long constructor instead")
    constructor(id: Int) : this(id.toSnapshotId())

    /**
     * Reference of the next state record. State records are stored in a linked list.
     *
     * Changes to [next] must preserve all existing records to all threads even during
     * intermediately changes. For example, it is safe to add the beginning or end of the list but
     * adding to the middle requires care. First the new record must have its [next] updated then
     * the [next] of its new predecessor can then be set to point to it. This implies that records
     * that are already in the list cannot be moved in the list as this the change must be atomic to
     * all threads that cannot happen without a lock which this list cannot afford.
     *
     * It is unsafe to remove a record as it might be in the process of being reused (see
     * [usedLocked]). If a record is removed care must be taken to ensure that it is not being
     * claimed by some other thread. This would require changes to [usedLocked].
     */
    internal var next: StateRecord? = null

    /** Copy the value into this state record from another for the same state object. */
    abstract fun assign(value: StateRecord)

    /**
     * Create a new state record for the same state object. Consider also implementing the [create]
     * overload that provides snapshotId for faster record construction when snapshot id is known.
     */
    abstract fun create(): StateRecord

    /**
     * Create a new state record for the same state object and provided [snapshotId]. This allows to
     * implement an optimized version of [create] to avoid accessing [currentSnapshot] when snapshot
     * id is known. The default implementation provides a backwards compatible behavior, and should
     * be overridden if [StateRecord] subclass supports this optimization.
     */
    @Deprecated("Use snapshotId: Long version instead", level = DeprecationLevel.HIDDEN)
    open fun create(snapshotId: Int): StateRecord =
        create().also { it.snapshotId = snapshotId.toSnapshotId() }

    /**
     * Create a new state record for the same state object and provided [snapshotId]. This allows to
     * implement an optimized version of [create] to avoid accessing [currentSnapshot] when snapshot
     * id is known. The default implementation provides a backwards compatible behavior, and should
     * be overridden if [StateRecord] subclass supports this optimization.
     */
    open fun create(snapshotId: SnapshotId): StateRecord =
        create().also { it.snapshotId = snapshotId }
}

/**
 * Interface implemented by all snapshot aware state objects. Used by this module to maintain the
 * state records of a state object.
 */
interface StateObject {
    /** The first state record in a linked list of state records. */
    val firstStateRecord: StateRecord

    /**
     * Add a new state record to the beginning of a list. After this call [firstStateRecord] should
     * be [value].
     */
    fun prependStateRecord(value: StateRecord)

    /**
     * Produce a merged state based on the conflicting state changes.
     *
     * This method must not modify any of the records received and should treat the state records as
     * immutable, even the [applied] record.
     *
     * @param previous the state record that was used to create the [applied] record and is a state
     *   that also (though indirectly) produced the [current] record.
     * @param current the state record of the parent snapshot or global state.
     * @param applied the state record that is being applied of the parent snapshot or global state.
     * @return the modified state or `null` if the values cannot be merged. If the states cannot be
     *   merged the current apply will fail. Any of the parameters can be returned as a result. If
     *   it is not one of the parameter values then it *must* be a new value that is created by
     *   calling [StateRecord.create] on one of the records passed and then can be modified to have
     *   the merged value before being returned. If a new record is returned [MutableSnapshot.apply]
     *   will update the internal snapshot id and call [prependStateRecord] if the record is used.
     */
    fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord,
    ): StateRecord? = null
}

/**
 * A snapshot whose state objects cannot be modified. If a state object is modified when in a
 * read-only snapshot a [IllegalStateException] is thrown.
 */
internal class ReadonlySnapshot
internal constructor(
    snapshotId: SnapshotId,
    invalid: SnapshotIdSet,
    override val readObserver: ((Any) -> Unit)?,
) : Snapshot(snapshotId, invalid) {
    /**
     * The number of nested snapshots that are active. To simplify the code, this snapshot counts
     * itself as a nested snapshot.
     */
    private var snapshots = 1
    override val readOnly: Boolean
        get() = true

    override val root: Snapshot
        get() = this

    override fun hasPendingChanges(): Boolean = false

    override val writeObserver: ((Any) -> Unit)?
        get() = null

    override var modified: MutableScatterSet<StateObject>?
        get() = null
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        validateOpen(this)
        return creatingSnapshot(
            parent = this,
            readObserver = readObserver,
            writeObserver = null,
            readonly = true,
        ) { actualReadObserver, _ ->
            NestedReadonlySnapshot(
                snapshotId = snapshotId,
                invalid = invalid,
                readObserver = mergedReadObserver(actualReadObserver, this.readObserver),
                parent = this,
            )
        }
    }

    override fun notifyObjectsInitialized() {
        // Nothing to do for read-only snapshots
    }

    override fun dispose() {
        if (!disposed) {
            nestedDeactivated(this)
            super.dispose()
            dispatchObserverOnPreDispose(this)
        }
    }

    override fun nestedActivated(snapshot: Snapshot) {
        snapshots++
    }

    override fun nestedDeactivated(snapshot: Snapshot) {
        if (--snapshots == 0) {
            // A read-only snapshot can be just be closed as it has no modifications.
            closeAndReleasePinning()
        }
    }

    override fun recordModified(state: StateObject) {
        reportReadonlySnapshotWrite()
    }
}

internal class NestedReadonlySnapshot(
    snapshotId: SnapshotId,
    invalid: SnapshotIdSet,
    override val readObserver: ((Any) -> Unit)?,
    val parent: Snapshot,
) : Snapshot(snapshotId, invalid) {
    init {
        parent.nestedActivated(this)
    }

    override val readOnly
        get() = true

    override val root: Snapshot
        get() = parent.root
    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?) =
        creatingSnapshot(
            parent = this,
            readObserver = readObserver,
            writeObserver = null,
            readonly = true,
        ) { actualReadObserver, _ ->
            NestedReadonlySnapshot(
                snapshotId = snapshotId,
                invalid = invalid,
                readObserver = mergedReadObserver(actualReadObserver, this.readObserver),
                parent = parent,
            )
        }

    override fun notifyObjectsInitialized() {
        // Nothing to do for read-only snapshots
    }

    override fun hasPendingChanges(): Boolean = false

    override fun dispose() {
        if (!disposed) {
            if (snapshotId != parent.snapshotId) {
                closeAndReleasePinning()
            }
            parent.nestedDeactivated(this)
            super.dispose()
            dispatchObserverOnPreDispose(this)
        }
    }

    override val modified: MutableScatterSet<StateObject>?
        get() = null

    override val writeObserver: ((Any) -> Unit)?
        get() = null

    override fun recordModified(state: StateObject) = reportReadonlySnapshotWrite()

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()

    override fun nestedActivated(snapshot: Snapshot) = unsupported()
}

private val emptyLambda: (invalid: SnapshotIdSet) -> Unit = {}

/**
 * A snapshot object that simplifies the code by treating the global state as a mutable snapshot.
 */
internal class GlobalSnapshot(snapshotId: SnapshotId, invalid: SnapshotIdSet) :
    MutableSnapshot(
        snapshotId,
        invalid,
        null,
        { state -> sync { globalWriteObservers.fastForEach { it(state) } } },
    ) {

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot =
        creatingSnapshot(
            parent = null,
            readonly = true,
            readObserver = readObserver,
            writeObserver = null,
        ) { actualReadObserver, _ ->
            takeNewSnapshot { invalid ->
                ReadonlySnapshot(
                    snapshotId = sync { nextSnapshotId.also { nextSnapshotId += 1 } },
                    invalid = invalid,
                    readObserver = actualReadObserver,
                )
            }
        }

    override fun takeNestedMutableSnapshot(
        readObserver: ((Any) -> Unit)?,
        writeObserver: ((Any) -> Unit)?,
    ): MutableSnapshot =
        creatingSnapshot(
            parent = null,
            readonly = false,
            readObserver = readObserver,
            writeObserver = writeObserver,
        ) { actualReadObserver, actualWriteObserver ->
            takeNewSnapshot { invalid ->
                MutableSnapshot(
                    snapshotId = sync { nextSnapshotId.also { nextSnapshotId += 1 } },
                    invalid = invalid,

                    // It is intentional that the global read observers are not merged with mutable
                    // snapshots read observers.
                    readObserver = actualReadObserver,

                    // It is intentional that global write observers are not merged with mutable
                    // snapshots write observers.
                    writeObserver = actualWriteObserver,
                )
            }
        }

    override fun notifyObjectsInitialized() {
        advanceGlobalSnapshot()
    }

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()

    override fun nestedActivated(snapshot: Snapshot) = unsupported()

    override fun apply(): SnapshotApplyResult =
        error("Cannot apply the global snapshot directly. Call Snapshot.advanceGlobalSnapshot")

    override fun dispose() {
        sync { releasePinnedSnapshotLocked() }
    }
}

/** A nested mutable snapshot created by [MutableSnapshot.takeNestedMutableSnapshot]. */
internal class NestedMutableSnapshot(
    snapshotId: SnapshotId,
    invalid: SnapshotIdSet,
    readObserver: ((Any) -> Unit)?,
    writeObserver: ((Any) -> Unit)?,
    val parent: MutableSnapshot,
) : MutableSnapshot(snapshotId, invalid, readObserver, writeObserver) {
    private var deactivated = false

    init {
        parent.nestedActivated(this)
    }

    override val root: Snapshot
        get() = parent.root

    override fun dispose() {
        if (!disposed) {
            super.dispose()
            deactivate()
        }
    }

    override fun apply(): SnapshotApplyResult {
        if (parent.applied || parent.disposed) return SnapshotApplyResult.Failure(this)

        // Applying a nested mutable snapshot applies its changes to the parent snapshot.

        // See MutableSnapshot.apply() for implantation notes.

        // The apply observer notification are for applying to the global scope so it is elided
        // here making this code a bit simpler than MutableSnapshot.apply.

        val modified = modified
        val id = snapshotId
        val optimisticMerges =
            if (modified != null) optimisticMerges(parent.snapshotId, this, parent.invalid)
            else null
        sync {
            validateOpen(this)
            if (modified == null || modified.size == 0) {
                closeAndReleasePinning()
            } else {
                val result =
                    innerApplyLocked(parent.snapshotId, modified, optimisticMerges, parent.invalid)
                if (result != SnapshotApplyResult.Success) return result

                parent.modified?.apply { addAll(modified) }
                    ?: modified.also {
                        // Ensure modified reference is only used by one snapshot
                        parent.modified = it
                        this.modified = null
                    }
            }

            // Ensure the parent is newer than the current snapshot
            if (parent.snapshotId < id) {
                parent.advance()
            }

            // Make the snapshot visible in the parent snapshot
            parent.invalid = parent.invalid.clear(id).andNot(previousIds)

            // Ensure the ids associated with this snapshot are also applied by the parent.
            parent.recordPrevious(id)
            parent.recordPreviousPinnedSnapshot(takeoverPinnedSnapshot())
            parent.recordPreviousList(previousIds)
            parent.recordPreviousPinnedSnapshots(previousPinnedSnapshots)
        }

        applied = true
        deactivate()
        dispatchObserverOnApplied(this, modified)
        return SnapshotApplyResult.Success
    }

    private fun deactivate() {
        if (!deactivated) {
            deactivated = true
            parent.nestedDeactivated(this)
        }
    }
}

/** A pseudo snapshot that doesn't introduce isolation but does introduce observers. */
internal class TransparentObserverMutableSnapshot(
    private val parentSnapshot: MutableSnapshot?,
    specifiedReadObserver: ((Any) -> Unit)?,
    specifiedWriteObserver: ((Any) -> Unit)?,
    private val mergeParentObservers: Boolean,
    private val ownsParentSnapshot: Boolean,
) :
    MutableSnapshot(
        INVALID_SNAPSHOT,
        SnapshotIdSet.EMPTY,
        mergedReadObserver(
            specifiedReadObserver,
            parentSnapshot?.readObserver ?: globalSnapshot.readObserver,
            mergeParentObservers,
        ),
        mergedWriteObserver(
            specifiedWriteObserver,
            parentSnapshot?.writeObserver ?: globalSnapshot.writeObserver,
        ),
    ) {
    override var readObserver: ((Any) -> Unit)? = super.readObserver
    override var writeObserver: ((Any) -> Unit)? = super.writeObserver

    internal val threadId: Long = currentThreadId()

    private val currentSnapshot: MutableSnapshot
        get() = parentSnapshot ?: globalSnapshot

    override fun dispose() {
        // Explicitly don't call super.dispose()
        disposed = true
        if (ownsParentSnapshot) {
            parentSnapshot?.dispose()
        }
    }

    override var snapshotId: SnapshotId
        get() = currentSnapshot.snapshotId
        @Suppress("UNUSED_PARAMETER")
        set(value) {
            unsupported()
        }

    override var invalid
        get() = currentSnapshot.invalid
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    override fun hasPendingChanges(): Boolean = currentSnapshot.hasPendingChanges()

    override var modified: MutableScatterSet<StateObject>?
        get() = currentSnapshot.modified
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    override var writeCount: Int
        get() = currentSnapshot.writeCount
        set(value) {
            currentSnapshot.writeCount = value
        }

    override val readOnly: Boolean
        get() = currentSnapshot.readOnly

    override fun apply(): SnapshotApplyResult = currentSnapshot.apply()

    override fun recordModified(state: StateObject) = currentSnapshot.recordModified(state)

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        val mergedReadObserver = mergedReadObserver(readObserver, this.readObserver)
        return if (!mergeParentObservers) {
            createTransparentSnapshotWithNoParentReadObserver(
                previousSnapshot = currentSnapshot.takeNestedSnapshot(null),
                readObserver = mergedReadObserver,
                ownsPreviousSnapshot = true,
            )
        } else {
            currentSnapshot.takeNestedSnapshot(mergedReadObserver)
        }
    }

    override fun takeNestedMutableSnapshot(
        readObserver: ((Any) -> Unit)?,
        writeObserver: ((Any) -> Unit)?,
    ): MutableSnapshot {
        val mergedReadObserver = mergedReadObserver(readObserver, this.readObserver)
        val mergedWriteObserver = mergedWriteObserver(writeObserver, this.writeObserver)
        return if (!mergeParentObservers) {
            val nestedSnapshot =
                currentSnapshot.takeNestedMutableSnapshot(
                    readObserver = null,
                    writeObserver = mergedWriteObserver,
                )
            TransparentObserverMutableSnapshot(
                parentSnapshot = nestedSnapshot,
                specifiedReadObserver = mergedReadObserver,
                specifiedWriteObserver = mergedWriteObserver,
                mergeParentObservers = false,
                ownsParentSnapshot = true,
            )
        } else {
            currentSnapshot.takeNestedMutableSnapshot(mergedReadObserver, mergedWriteObserver)
        }
    }

    override fun notifyObjectsInitialized() = currentSnapshot.notifyObjectsInitialized()

    /** Should never be called. */
    override fun nestedActivated(snapshot: Snapshot) = unsupported()

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()
}

/** A pseudo snapshot that doesn't introduce isolation but does introduce observers. */
internal class TransparentObserverSnapshot(
    private val parentSnapshot: Snapshot?,
    specifiedReadObserver: ((Any) -> Unit)?,
    private val mergeParentObservers: Boolean,
    private val ownsParentSnapshot: Boolean,
) : Snapshot(INVALID_SNAPSHOT, SnapshotIdSet.EMPTY) {
    override var readObserver: ((Any) -> Unit)? =
        mergedReadObserver(
            specifiedReadObserver,
            parentSnapshot?.readObserver ?: globalSnapshot.readObserver,
            mergeParentObservers,
        )
    override val writeObserver: ((Any) -> Unit)? = null

    internal val threadId: Long = currentThreadId()

    override val root: Snapshot = this

    private val currentSnapshot: Snapshot
        get() = parentSnapshot ?: globalSnapshot

    override fun dispose() {
        // Explicitly don't call super.dispose()
        disposed = true
        if (ownsParentSnapshot) {
            parentSnapshot?.dispose()
        }
    }

    override var snapshotId: SnapshotId
        get() = currentSnapshot.snapshotId
        @Suppress("UNUSED_PARAMETER")
        set(value) {
            unsupported()
        }

    override var invalid
        get() = currentSnapshot.invalid
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    override fun hasPendingChanges(): Boolean = currentSnapshot.hasPendingChanges()

    override var modified: MutableScatterSet<StateObject>?
        get() = currentSnapshot.modified
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    override val readOnly: Boolean
        get() = currentSnapshot.readOnly

    override fun recordModified(state: StateObject) = currentSnapshot.recordModified(state)

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        val mergedReadObserver = mergedReadObserver(readObserver, this.readObserver)
        return if (!mergeParentObservers) {
            createTransparentSnapshotWithNoParentReadObserver(
                currentSnapshot.takeNestedSnapshot(null),
                mergedReadObserver,
                ownsPreviousSnapshot = true,
            )
        } else {
            currentSnapshot.takeNestedSnapshot(mergedReadObserver)
        }
    }

    override fun notifyObjectsInitialized() = currentSnapshot.notifyObjectsInitialized()

    /** Should never be called. */
    override fun nestedActivated(snapshot: Snapshot) = unsupported()

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()
}

private fun createTransparentSnapshotWithNoParentReadObserver(
    previousSnapshot: Snapshot?,
    readObserver: ((Any) -> Unit)? = null,
    ownsPreviousSnapshot: Boolean = false,
): Snapshot =
    if (previousSnapshot is MutableSnapshot || previousSnapshot == null) {
        TransparentObserverMutableSnapshot(
            parentSnapshot = previousSnapshot as? MutableSnapshot,
            specifiedReadObserver = readObserver,
            specifiedWriteObserver = null,
            mergeParentObservers = false,
            ownsParentSnapshot = ownsPreviousSnapshot,
        )
    } else {
        TransparentObserverSnapshot(
            parentSnapshot = previousSnapshot,
            specifiedReadObserver = readObserver,
            mergeParentObservers = false,
            ownsParentSnapshot = ownsPreviousSnapshot,
        )
    }

private fun mergedReadObserver(
    readObserver: ((Any) -> Unit)?,
    parentObserver: ((Any) -> Unit)?,
    mergeReadObserver: Boolean = true,
): ((Any) -> Unit)? {
    @Suppress("NAME_SHADOWING") val parentObserver = if (mergeReadObserver) parentObserver else null
    return if (readObserver != null && parentObserver != null && readObserver !== parentObserver) {
        { state: Any ->
            readObserver(state)
            parentObserver(state)
        }
    } else readObserver ?: parentObserver
}

private fun mergedWriteObserver(
    writeObserver: ((Any) -> Unit)?,
    parentObserver: ((Any) -> Unit)?,
): ((Any) -> Unit)? =
    if (writeObserver != null && parentObserver != null && writeObserver !== parentObserver) {
        { state: Any ->
            writeObserver(state)
            parentObserver(state)
        }
    } else writeObserver ?: parentObserver

/**
 * Snapshot id of `0` is reserved as invalid and no state record with snapshot `0` is considered
 * valid.
 *
 * The value `0` was chosen as it is the default value of the Int snapshot id type and records
 * initially created will naturally have a snapshot id of 0. If this wasn't considered invalid
 * adding such a record to a state object will make the state record immediately visible to the
 * snapshots instead of being born invalid. Using `0` ensures all state records are created invalid
 * and must be explicitly marked as valid in to be visible in a snapshot.
 */
private const val INVALID_SNAPSHOT = SnapshotIdZero

/** Current thread snapshot */
private val threadSnapshot = SnapshotThreadLocal<Snapshot>()

/**
 * A global synchronization object. This synchronization object should be taken before modifying any
 * of the fields below.
 */
@PublishedApi internal val lock: SynchronizedObject = makeSynchronizedObject()

@Suppress("BanInlineOptIn", "LEAKED_IN_PLACE_LAMBDA", "WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
@PublishedApi
internal inline fun <T> sync(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return synchronized(lock, block)
}

// The following variables should only be written when sync is taken

/**
 * A set of snapshots that are currently open and should be considered invalid for new snapshots.
 */
private var openSnapshots = SnapshotIdSet.EMPTY

/** The first snapshot created must be at least on more than the [Snapshot.PreexistingSnapshotId] */
private var nextSnapshotId = Snapshot.PreexistingSnapshotId.toSnapshotId() + 1

/**
 * A tracking table for pinned snapshots. A pinned snapshot is the lowest snapshot id that the
 * snapshot is ignoring by considering them invalid. This is used to calculate when a snapshot
 * record can be reused.
 */
private val pinningTable = SnapshotDoubleIndexHeap()

/**
 * The set of objects who have more than one active state record. These are traversed during apply
 * of mutable snapshots and when the global snapshot is advanced to determine if any of the records
 * can be cleared.
 */
private val extraStateObjects = SnapshotWeakSet<StateObject>()

/** A list of apply observers */
private var applyObservers = emptyList<(Set<Any>, Snapshot) -> Unit>()

/** A list of observers of writes to the global state. */
private var globalWriteObservers = emptyList<(Any) -> Unit>()

private val globalSnapshot =
    GlobalSnapshot(
        snapshotId = nextSnapshotId.also { nextSnapshotId += 1 },
        invalid = SnapshotIdSet.EMPTY,
    )
        .also { openSnapshots = openSnapshots.set(it.snapshotId) }

// Unused, kept for API compat
@Suppress("unused") @PublishedApi internal val snapshotInitializer: Snapshot = globalSnapshot

private fun <T> resetGlobalSnapshotLocked(
    globalSnapshot: GlobalSnapshot,
    block: (invalid: SnapshotIdSet) -> T,
): T {
    val snapshotId = globalSnapshot.snapshotId
    val result = block(openSnapshots.clear(snapshotId))

    val nextGlobalSnapshotId = nextSnapshotId
    nextSnapshotId += 1

    openSnapshots = openSnapshots.clear(snapshotId)
    globalSnapshot.snapshotId = nextGlobalSnapshotId
    globalSnapshot.invalid = openSnapshots
    globalSnapshot.writeCount = 0
    globalSnapshot.modified = null
    globalSnapshot.releasePinnedSnapshotLocked()
    openSnapshots = openSnapshots.set(nextGlobalSnapshotId)

    return result
}

/**
 * Counts the number of threads currently inside `advanceGlobalSnapshot`, notifying observers of
 * changes to the global snapshot.
 */
private var pendingApplyObserverCount = AtomicInt(0)

private fun <T> advanceGlobalSnapshot(block: (invalid: SnapshotIdSet) -> T): T {
    val globalSnapshot = globalSnapshot

    val modified: MutableScatterSet<StateObject>?
    val result = sync {
        modified = globalSnapshot.modified
        if (modified != null) {
            pendingApplyObserverCount.add(1)
        }
        resetGlobalSnapshotLocked(globalSnapshot, block)
    }

    // If the previous global snapshot had any modified states then notify the registered apply
    // observers.
    modified?.let {
        try {
            val observers = applyObservers
            observers.fastForEach { observer -> observer(it.wrapIntoSet(), globalSnapshot) }
        } finally {
            pendingApplyObserverCount.add(-1)
        }
    }

    sync {
        checkAndOverwriteUnusedRecordsLocked()
        modified?.forEach { processForUnusedRecordsLocked(it) }
    }

    return result
}

private fun advanceGlobalSnapshot() = advanceGlobalSnapshot(emptyLambda)

private fun <T : Snapshot> takeNewSnapshot(block: (invalid: SnapshotIdSet) -> T): T =
    advanceGlobalSnapshot { invalid ->
        val result = block(invalid)
        sync { openSnapshots = openSnapshots.set(result.snapshotId) }
        result
    }

private fun validateOpen(snapshot: Snapshot) {
    val openSnapshots = openSnapshots
    if (!openSnapshots.get(snapshot.snapshotId)) {
        error(
            "Snapshot is not open: snapshotId=${
                snapshot.snapshotId
            }, disposed=${
                snapshot.disposed
            }, applied=${
                (snapshot as? MutableSnapshot)?.applied ?: "read-only"
            }, lowestPin=${
                sync { pinningTable.lowestOrDefault(SnapshotIdInvalidValue) }
            }"
        )
    }
}

/**
 * A candidate snapshot is valid if the it is less than or equal to the current snapshot and it
 * wasn't specifically marked as invalid when the snapshot started.
 *
 * All snapshot active at when the snapshot was taken considered invalid for the snapshot (they have
 * not been applied and therefore are considered invalid).
 *
 * All snapshots taken after the current snapshot are considered invalid since they where taken
 * after the current snapshot was taken.
 *
 * INVALID_SNAPSHOT is reserved as an invalid snapshot id.
 */
private fun valid(
    currentSnapshot: SnapshotId,
    candidateSnapshot: SnapshotId,
    invalid: SnapshotIdSet,
): Boolean {
    return candidateSnapshot != INVALID_SNAPSHOT &&
            candidateSnapshot <= currentSnapshot &&
            !invalid.get(candidateSnapshot)
}

// Determine if the given data is valid for the snapshot.
private fun valid(data: StateRecord, snapshot: SnapshotId, invalid: SnapshotIdSet): Boolean {
    return valid(snapshot, data.snapshotId, invalid)
}

private fun <T : StateRecord> readable(r: T, id: SnapshotId, invalid: SnapshotIdSet): T? {
    // The readable record is the valid record with the highest snapshotId
    var current: StateRecord? = r
    var candidate: StateRecord? = null
    while (current != null) {
        if (valid(current, id, invalid)) {
            candidate =
                if (candidate == null) current
                else if (candidate.snapshotId < current.snapshotId) current else candidate
        }
        current = current.next
    }
    if (candidate != null) {
        @Suppress("UNCHECKED_CAST")
        return candidate as T
    }
    return null
}

/**
 * Return the current readable state record for the current snapshot. It is assumed that [this] is
 * the first record of [state]
 */
fun <T : StateRecord> T.readable(state: StateObject): T {
    val snapshot = Snapshot.current
    snapshot.readObserver?.invoke(state)
    return readable(this, snapshot.snapshotId, snapshot.invalid)
        ?: sync {
            // Readable can return null when the global snapshot has been advanced by another thread
            // and state written to the object was overwritten while this thread was paused.
            // Repeating the read is valid here as either this will return the same result as
            // the previous call or will find a valid record. Being in a sync block prevents other
            // threads from writing to this state object until the read completes.
            val syncSnapshot = Snapshot.current
            @Suppress("UNCHECKED_CAST")
            readable(state.firstStateRecord as T, syncSnapshot.snapshotId, syncSnapshot.invalid)
                ?: readError()
        }
}

// unused, still here for API compat.
/**
 * Return the current readable state record for the [snapshot]. It is assumed that [this] is the
 * first record of [state]
 */
fun <T : StateRecord> T.readable(state: StateObject, snapshot: Snapshot): T {
    // invoke the observer associated with the current snapshot.
    snapshot.readObserver?.invoke(state)
    return readable(this, snapshot.snapshotId, snapshot.invalid)
        ?: sync {
            // Readable can return null when the global snapshot has been advanced by another thread
            // See T.readable(state: StateObject) for more info.
            val syncSnapshot = Snapshot.current
            @Suppress("UNCHECKED_CAST")
            readable(state.firstStateRecord as T, syncSnapshot.snapshotId, syncSnapshot.invalid)
                ?: readError()
        }
}

private fun readError(): Nothing {
    error(
        "Reading a state that was created after the snapshot was taken or in a snapshot that " +
                "has not yet been applied"
    )
}

/**
 * A record can be reused if no other snapshot will see it as valid. This is always true for a
 * record created in an abandoned snapshot. It is also true if the record is valid in the previous
 * snapshot and is obscured by another record also valid in the previous state record.
 */
private fun usedLocked(state: StateObject): StateRecord? {
    var current: StateRecord? = state.firstStateRecord
    var validRecord: StateRecord? = null
    val reuseLimit = pinningTable.lowestOrDefault(nextSnapshotId) - 1
    val invalid = SnapshotIdSet.EMPTY
    while (current != null) {
        val currentId = current.snapshotId
        if (currentId == INVALID_SNAPSHOT) {
            // Any records that were marked invalid by an abandoned snapshot or is marked reachable
            // can be used immediately.
            return current
        }
        if (valid(current, reuseLimit, invalid)) {
            if (validRecord == null) {
                validRecord = current
            } else {
                // If we have two valid records one must obscure the other. Return the
                // record with the lowest id
                return if (current.snapshotId < validRecord.snapshotId) current else validRecord
            }
        }
        current = current.next
    }
    return null
}

/**
 * Clear records that cannot be selected in any currently open snapshot.
 *
 * This method uses the same technique as [usedLocked] which uses the [pinningTable] to determine
 * lowest id in the invalid set for all snapshots. Only the record with the greatest id of all
 * records less or equal to this lowest id can possibly be selected in any snapshot and all other
 * records below that number can be overwritten.
 *
 * However, this technique doesn't find all records that will not be selected by any open snapshot
 * as a record that has an id above that number could be reusable but will not be found.
 *
 * For example if snapshot 1 is open and 2 is created and modifies [state] then is applied, 3 is
 * open and then 4 is open, and then 1 is applied. When 3 modifies [state] and then applies, as 1 is
 * pinned by 4, it is uncertain whether the record for 2 is needed by 4 so it must be kept even if 4
 * also modified [state] and would not select 2. Accurately determine if a record is selectable
 * would require keeping a list of all open [Snapshot] instances which currently is not kept and
 * traversing that list for each record.
 *
 * If any such records are possible this method returns true. In other words, this method returns
 * true if any records might be reusable but this function could not prove there were or not.
 */
private fun overwriteUnusedRecordsLocked(state: StateObject): Boolean {
    var current: StateRecord? = state.firstStateRecord
    var overwriteRecord: StateRecord? = null
    var validRecord: StateRecord? = null
    val reuseLimit = pinningTable.lowestOrDefault(nextSnapshotId)
    var retainedRecords = 0

    while (current != null) {
        val currentId = current.snapshotId
        if (currentId != INVALID_SNAPSHOT) {
            if (currentId < reuseLimit) {
                if (validRecord == null) {
                    // If any records are below [reuseLimit] then we must keep the highest one
                    // so the lowest snapshot can select it.
                    validRecord = current
                    retainedRecords++
                } else {
                    // If [validRecord] is from an earlier snapshot, overwrite it instead
                    val recordToOverwrite =
                        if (current.snapshotId < validRecord.snapshotId) {
                            current
                        } else {
                            // We cannot use `.also { }` here as it prevents smart casting of other
                            // uses of [validRecord].
                            val result = validRecord
                            validRecord = current
                            result
                        }
                    if (overwriteRecord == null) {
                        // Find a record we will definitely keep
                        overwriteRecord =
                            state.firstStateRecord.findYoungestOr { it.snapshotId >= reuseLimit }
                    }
                    recordToOverwrite.snapshotId = INVALID_SNAPSHOT
                    recordToOverwrite.assign(overwriteRecord)
                }
            } else {
                retainedRecords++
            }
        }
        current = current.next
    }

    return retainedRecords > 1
}

private inline fun StateRecord.findYoungestOr(predicate: (StateRecord) -> Boolean): StateRecord {
    var current: StateRecord? = this
    var youngest = this
    while (current != null) {
        if (predicate(current)) return current
        if (youngest.snapshotId < current.snapshotId) youngest = current
        current = current.next
    }
    return youngest
}

private fun checkAndOverwriteUnusedRecordsLocked() {
    extraStateObjects.removeIf { !overwriteUnusedRecordsLocked(it) }
}

private fun processForUnusedRecordsLocked(state: StateObject) {
    if (overwriteUnusedRecordsLocked(state)) {
        extraStateObjects.add(state)
    }
}

@PublishedApi
internal fun <T : StateRecord> T.writableRecord(state: StateObject, snapshot: Snapshot): T {
    if (snapshot.readOnly) {
        // If the snapshot is read-only, use the snapshot recordModified to report it.
        snapshot.recordModified(state)
    }
    val id = snapshot.snapshotId
    val readData = readable(this, id, snapshot.invalid) ?: readError()

    // If the readable data was born in this snapshot, it is writable.
    if (readData.snapshotId == snapshot.snapshotId) return readData

    // Otherwise, make a copy of the readable data and mark it as born in this snapshot, making it
    // writable.
    @Suppress("UNCHECKED_CAST")
    val newData =
        sync {
            // Verify that some other thread didn't already create this.
            val newReadData = readable(state.firstStateRecord, id, snapshot.invalid) ?: readError()
            if (newReadData.snapshotId == id) newReadData
            else newReadData.newWritableRecordLocked(state, snapshot)
        }
                as T

    if (readData.snapshotId != Snapshot.PreexistingSnapshotId.toSnapshotId()) {
        snapshot.recordModified(state)
    }

    return newData
}

internal fun <T : StateRecord> T.overwritableRecord(
    state: StateObject,
    snapshot: Snapshot,
    candidate: T,
): T {
    if (snapshot.readOnly) {
        // If the snapshot is read-only, use the snapshot recordModified to report it.
        snapshot.recordModified(state)
    }
    val id = snapshot.snapshotId

    if (candidate.snapshotId == id) return candidate

    val newData = sync { newOverwritableRecordLocked(state) }
    newData.snapshotId = id

    if (candidate.snapshotId != Snapshot.PreexistingSnapshotId.toSnapshotId()) {
        snapshot.recordModified(state)
    }

    return newData
}

internal fun <T : StateRecord> T.newWritableRecord(state: StateObject, snapshot: Snapshot) = sync {
    newWritableRecordLocked(state, snapshot)
}

private fun <T : StateRecord> T.newWritableRecordLocked(state: StateObject, snapshot: Snapshot): T {
    // Calling used() on a state object might return the same record for each thread calling
    // used() therefore selecting the record to reuse should be guarded.

    // Note: setting the snapshotId to Int.MAX_VALUE will make it invalid for all snapshots.
    // This means the lock can be released as used() will no longer select it. Using id could
    // also be used but it puts the object into a state where the reused value appears to be
    // the current valid value for the snapshot. This is not an issue if the snapshot is only
    // being read from a single thread but using Int.MAX_VALUE allows multiple readers,
    // single writer, of a snapshot. Note that threads reading a mutating snapshot should not
    // cache the result of readable() as the mutating thread calls to writable() can change the
    // result of readable().
    val newData = newOverwritableRecordLocked(state)
    newData.assign(this)
    newData.snapshotId = snapshot.snapshotId
    return newData
}

internal fun <T : StateRecord> T.newOverwritableRecordLocked(state: StateObject): T {
    // Calling used() on a state object might return the same record for each thread calling
    // used() therefore selecting the record to reuse should be guarded.

    // Note: setting the snapshotId to Int.MAX_VALUE will make it invalid for all snapshots.
    // This means the lock can be released as used() will no longer select it. Using id could
    // also be used but it puts the object into a state where the reused value appears to be
    // the current valid value for the snapshot. This is not an issue if the snapshot is only
    // being read from a single thread but using Int.MAX_VALUE allows multiple readers,
    // single writer, of a snapshot. Note that threads reading a mutating snapshot should not
    // cache the result of readable() as the mutating thread calls to writable() can change the
    // result of readable().
    @Suppress("UNCHECKED_CAST")
    return (usedLocked(state) as T?)?.apply { snapshotId = SnapshotIdMax }
        ?: create(SnapshotIdMax).apply {
            this.next = state.firstStateRecord
            state.prependStateRecord(this as T)
        } as T
}

@PublishedApi
internal fun notifyWrite(snapshot: Snapshot, state: StateObject) {
    snapshot.writeCount += 1
    snapshot.writeObserver?.invoke(state)
}

/**
 * Call [block] with a writable state record for [snapshot] of the given record. It is assumed that
 * this is called for the first state record in a state object. If the snapshot is read-only calling
 * this will throw.
 */
inline fun <T : StateRecord, R> T.writable(
    state: StateObject,
    snapshot: Snapshot,
    block: T.() -> R,
): R {
    // A writable record will always be the readable record (as all newer records are invalid it
    // must be the newest valid record). This means that if the readable record is not from the
    // current snapshot, a new record must be created. To create a new writable record, a record
    // can be reused, if possible, and the readable record is applied to it. If a record cannot
    // be reused, a new record is created and the readable record is applied to it. Once the
    // values are correct the record is made live by giving it the current snapshot id.

    // Writes need to be in a `sync` block as all writes in flight must be completed before a new
    // snapshot is take. Writing in a sync block ensures this is the case because new snapshots
    // are also in a sync block.
    return sync { this.writableRecord(state, snapshot).block() }
        .also { notifyWrite(snapshot, state) }
}

/**
 * Call [block] with a writable state record for the given record. It is assumed that this is called
 * for the first state record in a state object. A record is writable if it was created in the
 * current mutable snapshot.
 */
inline fun <T : StateRecord, R> T.writable(state: StateObject, block: T.() -> R): R {
    val snapshot: Snapshot
    return sync {
        snapshot = Snapshot.current
        this.writableRecord(state, snapshot).block()
    }
        .also { notifyWrite(snapshot, state) }
}

/**
 * Call [block] with a writable state record for the given record. It is assumed that this is called
 * for the first state record in a state object. A record is writable if it was created in the
 * current mutable snapshot. This should only be used when the record will be overwritten in its
 * entirety (such as having only one field and that field is written to).
 *
 * WARNING: If the caller doesn't overwrite all the fields in the state record the object will be
 * inconsistent and the fields not written are almost guaranteed to be incorrect. If it is possible
 * that [block] will not write to all the fields use [writable] instead.
 *
 * @param state The object that has this record in its record list.
 * @param candidate The current for the snapshot record returned by [withCurrent]
 * @param block The block that will mutate all the field of the record.
 */
internal inline fun <T : StateRecord, R> T.overwritable(
    state: StateObject,
    candidate: T,
    block: T.() -> R,
): R {
    val snapshot: Snapshot
    return sync {
        snapshot = Snapshot.current
        this.overwritableRecord(state, snapshot, candidate).block()
    }
        .also { notifyWrite(snapshot, state) }
}

/**
 * Produce a set of optimistic merges of the state records, this is performed outside the a
 * synchronization block to reduce the amount of time taken in the synchronization block reducing
 * the thread contention of merging state values.
 */
private fun optimisticMerges(
    currentSnapshotId: SnapshotId,
    applyingSnapshot: MutableSnapshot,
    invalidSnapshots: SnapshotIdSet,
): Map<StateRecord, StateRecord>? {
    val modified = applyingSnapshot.modified
    if (modified == null) return null
    val start =
        applyingSnapshot.invalid.set(applyingSnapshot.snapshotId).or(applyingSnapshot.previousIds)
    var result: MutableMap<StateRecord, StateRecord>? = null
    modified.forEach { state ->
        val first = state.firstStateRecord
        val current = readable(first, currentSnapshotId, invalidSnapshots) ?: return@forEach
        val previous = readable(first, currentSnapshotId, start) ?: return@forEach
        if (current != previous) {
            // Try to produce a merged state record
            val applied =
                readable(first, applyingSnapshot.snapshotId, applyingSnapshot.invalid)
                    ?: readError()
            val merged = state.mergeRecords(previous, current, applied)
            if (merged != null) {
                (result ?: hashMapOf<StateRecord, StateRecord>().also { result = it })[current] =
                    merged
            } else {
                // If one fails don't bother calculating the others as they are likely not going
                // to be used. There is an unlikely case that a optimistic merge cannot be
                // produced but the snapshot will apply because, once the synchronization is taken,
                // the current state can be merge. This routine errors on the side of reduced
                // overall work by not performing work that is likely to be ignored.
                return null
            }
        }
    }
    return result
}

private fun reportReadonlySnapshotWrite(): Nothing {
    error("Cannot modify a state object in a read-only snapshot")
}

/** Returns the current record without notifying any read observers. */
@PublishedApi
internal fun <T : StateRecord> current(r: T, snapshot: Snapshot): T =
    readable(r, snapshot.snapshotId, snapshot.invalid)
        ?: sync {
            // Global snapshot could have been advanced
            // see StateRecord.readable for more details
            readable(r, snapshot.snapshotId, snapshot.invalid)
        }
        ?: readError()

@PublishedApi
internal fun <T : StateRecord> current(r: T): T =
    Snapshot.current.let { snapshot ->
        readable(r, snapshot.snapshotId, snapshot.invalid)
            ?: sync {
                // Global snapshot could have been advanced
                // see StateRecord.readable for more details
                Snapshot.current.let { syncSnapshot ->
                    readable(r, syncSnapshot.snapshotId, syncSnapshot.invalid)
                }
            }
            ?: readError()
    }

/**
 * Provides a [block] with the current record, without notifying any read observers.
 *
 * @see readable
 */
inline fun <T : StateRecord, R> T.withCurrent(block: (r: T) -> R): R = block(current(this))

/** Helper routine to add a range of values ot a snapshot set */
internal fun SnapshotIdSet.addRange(from: SnapshotId, until: SnapshotId): SnapshotIdSet {
    var result = this
    var invalidId = from
    while (invalidId < until) {
        result = result.set(invalidId)
        invalidId += 1
    }
    return result
}