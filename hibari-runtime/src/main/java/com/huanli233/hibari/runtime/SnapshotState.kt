/*
 * Copyright 2019 The Android Open Source Project
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

@file:JvmName("SnapshotStateKt")
@file:JvmMultifileClass

package com.huanli233.hibari.runtime

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.huanli233.hibari.runtime.snapshots.GlobalSnapshot
import com.huanli233.hibari.runtime.snapshots.Snapshot
import com.huanli233.hibari.runtime.snapshots.SnapshotId
import com.huanli233.hibari.runtime.snapshots.SnapshotMutableState
import com.huanli233.hibari.runtime.snapshots.SnapshotStateList
import com.huanli233.hibari.runtime.snapshots.SnapshotStateMap
import com.huanli233.hibari.runtime.snapshots.SnapshotStateSet
import com.huanli233.hibari.runtime.snapshots.StateFactoryMarker
import com.huanli233.hibari.runtime.snapshots.StateObjectImpl
import com.huanli233.hibari.runtime.snapshots.StateRecord
import com.huanli233.hibari.runtime.snapshots.currentSnapshot
import com.huanli233.hibari.runtime.snapshots.overwritable
import com.huanli233.hibari.runtime.snapshots.readable
import com.huanli233.hibari.runtime.snapshots.toSnapshotId
import com.huanli233.hibari.runtime.snapshots.withCurrent
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

/**
 * Return a new [MutableState] initialized with the passed in [value]
 *
 * The MutableState class is a single value holder whose reads and writes are observed by Compose.
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the initial value for the [MutableState]
 * @param policy a policy to controls how changes are handled in mutable snapshots.
 */
@StateFactoryMarker
fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
): MutableState<T> = createSnapshotMutableState(value, policy)

/**
 * A value holder where reads to the [value] property during the execution of a [Tunable]
 * function, the current [RetuneScope] will be subscribed to changes of that value.
 *
 * @see [MutableState]
 * @see [mutableStateOf]
 */
@Stable
interface State<out T> {
    val value: T
}

/**
 * Permits property delegation of `val`s using `by` for [State].
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

/**
 * A mutable value holder where reads to the [value] property during the execution of a [Tunable]
 * function, the current [RetuneScope] will be subscribed to changes of that value. When the
 * [value] property is written to and changed, a recomposition of any subscribed [RetuneScope]s
 * will be scheduled. If [value] is written to with the same value, no recompositions will be
 * scheduled.
 *
 * @see [State]
 * @see [mutableStateOf]
 */
@Stable
interface MutableState<T> : State<T> {
    override var value: T

    operator fun component1(): T

    operator fun component2(): (T) -> Unit
}

/**
 * Permits property delegation of `var`s using `by` for [MutableState].
 *
 * @sample androidx.compose.runtime.samples.DelegatedStateSample
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> MutableState<T>.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: T,
) {
    this.value = value
}

/** Returns platform specific implementation based on [SnapshotMutableStateImpl]. */
internal fun <T> createSnapshotMutableState(
    value: T,
    policy: SnapshotMutationPolicy<T>,
): SnapshotMutableState<T> = ParcelableSnapshotMutableState(value, policy)

@SuppressLint("BanParcelableUsage")
private class ParcelableSnapshotMutableState<T>(value: T, policy: SnapshotMutationPolicy<T>) :
    SnapshotMutableStateImpl<T>(value, policy), Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(value)
        parcel.writeInt(
            when (policy) {
                neverEqualPolicy<Any?>() -> PolicyNeverEquals
                structuralEqualityPolicy<Any?>() -> PolicyStructuralEquality
                referentialEqualityPolicy<Any?>() -> PolicyReferentialEquality
                else ->
                    throw IllegalStateException(
                        "Only known types of MutableState's SnapshotMutationPolicy are supported"
                    )
            }
        )
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private const val PolicyNeverEquals = 0
        private const val PolicyStructuralEquality = 1
        private const val PolicyReferentialEquality = 2

        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableSnapshotMutableState<Any?>> =
            object : Parcelable.ClassLoaderCreator<ParcelableSnapshotMutableState<Any?>> {
                override fun createFromParcel(
                    parcel: Parcel,
                    loader: ClassLoader?,
                ): ParcelableSnapshotMutableState<Any?> {
                    val value = parcel.readValue(loader ?: javaClass.classLoader)
                    val policyIndex = parcel.readInt()
                    return ParcelableSnapshotMutableState(
                        value,
                        when (policyIndex) {
                            PolicyNeverEquals -> neverEqualPolicy()
                            PolicyStructuralEquality -> structuralEqualityPolicy()
                            PolicyReferentialEquality -> referentialEqualityPolicy()
                            else ->
                                throw IllegalStateException(
                                    "Unsupported MutableState policy $policyIndex was restored"
                                )
                        },
                    )
                }

                override fun createFromParcel(parcel: Parcel) = createFromParcel(parcel, null)

                override fun newArray(size: Int) =
                    arrayOfNulls<ParcelableSnapshotMutableState<Any?>?>(size)
            }
    }
}

/**
 * A single value holder whose reads and writes are observed by Compose.
 *
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the wrapped value
 * @param policy a policy to control how changes are handled in a mutable snapshot.
 * @see mutableStateOf
 * @see SnapshotMutationPolicy
 */
internal open class SnapshotMutableStateImpl<T>(
    value: T,
    override val policy: SnapshotMutationPolicy<T>,
) : StateObjectImpl(), SnapshotMutableState<T> {
    @Suppress("UNCHECKED_CAST")
    override var value: T
        get() = next.readable(this).value
        set(value) =
            next.withCurrent {
                if (!policy.equivalent(it.value, value)) {
                    next.overwritable(this, it) { this.value = value }
                }
            }

    private var next: StateStateRecord<T> =
        currentSnapshot().let { snapshot ->
            StateStateRecord(snapshot.snapshotId, value).also {
                if (snapshot !is GlobalSnapshot) {
                    it.next = StateStateRecord(Snapshot.PreexistingSnapshotId.toSnapshotId(), value)
                }
            }
        }

    override val firstStateRecord: StateRecord
        get() = next

    override fun prependStateRecord(value: StateRecord) {
        @Suppress("UNCHECKED_CAST")
        next = value as StateStateRecord<T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord,
    ): StateRecord? {
        val previousRecord = previous as StateStateRecord<T>
        val currentRecord = current as StateStateRecord<T>
        val appliedRecord = applied as StateStateRecord<T>
        return if (policy.equivalent(currentRecord.value, appliedRecord.value)) current
        else {
            val merged =
                policy.merge(previousRecord.value, currentRecord.value, appliedRecord.value)
            if (merged != null) {
                appliedRecord.create(appliedRecord.snapshotId).also { it.value = merged }
            } else {
                null
            }
        }
    }

    override fun toString(): String =
        next.withCurrent { "MutableState(value=${it.value})@${hashCode()}" }

    private class StateStateRecord<T>(snapshotId: SnapshotId, myValue: T) :
        StateRecord(snapshotId) {
        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST")
            this.value = (value as StateStateRecord<T>).value
        }

        override fun create() = StateStateRecord(currentSnapshot().snapshotId, value)

        override fun create(snapshotId: SnapshotId) =
            StateStateRecord(currentSnapshot().snapshotId, value)

        var value: T = myValue
    }

    /**
     * The componentN() operators allow state objects to be used with the property destructuring
     * syntax
     *
     * ```
     * var (foo, setFoo) = remember { mutableStateOf(0) }
     * setFoo(123) // set
     * foo == 123 // get
     * ```
     */
    override operator fun component1(): T = value

    override operator fun component2(): (T) -> Unit = { value = it }

    /**
     * A function used by the debugger to display the value of the current value of the mutable
     * state object without triggering read observers.
     */
    @Suppress("unused")
    val debuggerDisplayValue: T
        @JvmName("getDebuggerDisplayValue") get() = next.withCurrent { it }.value
}

/**
 * Create a instance of [MutableList]<T> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableListOf
 * @see MutableList
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
fun <T> mutableStateListOf(): SnapshotStateList<T> = SnapshotStateList()

/**
 * Create an instance of [MutableList]<T> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableListOf
 * @see MutableList
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
fun <T> mutableStateListOf(vararg elements: T): SnapshotStateList<T> =
    SnapshotStateList<T>().also { it.addAll(elements.toList()) }

/**
 * Create an instance of [MutableList]<T> from a collection that is observable and can be snapshot.
 */
fun <T> Collection<T>.toMutableStateList() = SnapshotStateList<T>().also { it.addAll(this) }

/**
 * Create a instance of [MutableMap]<K, V> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableMapOf
 * @see MutableMap
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
fun <K, V> mutableStateMapOf(): SnapshotStateMap<K, V> = SnapshotStateMap()

/**
 * Create a instance of [MutableMap]<K, V> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableMapOf
 * @see MutableMap
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
fun <K, V> mutableStateMapOf(vararg pairs: Pair<K, V>): SnapshotStateMap<K, V> =
    SnapshotStateMap<K, V>().apply { putAll(pairs.toMap()) }

/**
 * Create an instance of [MutableMap]<K, V> from a collection of pairs that is observable and can be
 * snapshot.
 */
@Suppress("unused")
fun <K, V> Iterable<Pair<K, V>>.toMutableStateMap() =
    SnapshotStateMap<K, V>().also { it.putAll(this.toMap()) }

/**
 * Create a instance of [MutableSet]<T> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableSetOf
 * @see MutableSet
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
fun <T> mutableStateSetOf(): SnapshotStateSet<T> = SnapshotStateSet()

/**
 * Create an instance of [MutableSet]<T> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableSetOf
 * @see MutableSet
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
fun <T> mutableStateSetOf(vararg elements: T): SnapshotStateSet<T> =
    SnapshotStateSet<T>().also { it.addAll(elements.toSet()) }

@Tunable
fun <T> rememberUpdatedState(newValue: T): State<T> = remember {
    mutableStateOf(newValue)
}.apply { value = newValue }