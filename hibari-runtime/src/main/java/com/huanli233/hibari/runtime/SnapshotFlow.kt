package com.huanli233.hibari.runtime

import androidx.collection.MutableScatterSet
import com.huanli233.hibari.runtime.collection.fastAny
import com.huanli233.hibari.runtime.snapshots.ReaderKind
import com.huanli233.hibari.runtime.snapshots.Snapshot
import com.huanli233.hibari.runtime.snapshots.StateObjectImpl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Tunable
fun <T> StateFlow<T>.collectAsState(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = collectAsState(value, context)

@Tunable
fun <T : R, R> Flow<T>.collectAsState(
    initial: R,
    context: CoroutineContext = EmptyCoroutineContext
): State<R> = produceState(initial, this, context) {
    if (context == EmptyCoroutineContext) {
        collect { value = it }
    } else withContext(context) {
        collect { value = it }
    }
}

fun <T> snapshotFlow(
    block: () -> T
): Flow<T> = flow {
    val readSet = MutableScatterSet<Any>()
    val readObserver: (Any) -> Unit = {
        if (it is StateObjectImpl) {
            it.recordReadIn(ReaderKind.SnapshotFlow)
        }
        readSet.add(it)
    }

    // This channel may not block or lose data on a trySend call.
    val appliedChanges = Channel<Set<Any>>(Channel.UNLIMITED)

    // Register the apply observer before running for the first time
    // so that we don't miss updates.
    val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
        val maybeObserved = changed.fastAny {
            it !is StateObjectImpl || it.isReadIn(ReaderKind.SnapshotFlow)
        }

        if (maybeObserved) {
            appliedChanges.trySend(changed)
        }
    }

    try {
        var lastValue = Snapshot.takeSnapshot(readObserver).run {
            try {
                enter(block)
            } finally {
                dispose()
            }
        }
        emit(lastValue)

        while (true) {
            var found = false
            var changedObjects = appliedChanges.receive()

            // Poll for any other changes before running block to minimize the number of
            // additional times it runs for the same data
            while (true) {
                // Assumption: readSet will typically be smaller than changed set
                found = found || readSet.intersects(changedObjects)
                changedObjects = appliedChanges.tryReceive().getOrNull() ?: break
            }

            if (found) {
                readSet.clear()
                val newValue = Snapshot.takeSnapshot(readObserver).run {
                    try {
                        enter(block)
                    } finally {
                        dispose()
                    }
                }

                if (newValue != lastValue) {
                    lastValue = newValue
                    emit(newValue)
                }
            }
        }
    } finally {
        unregisterApplyObserver.dispose()
    }
}

private fun MutableScatterSet<Any>.intersects(set: Set<Any>) =
    any { it in set }