package com.huanli233.hibari.runtime

import com.huanli233.hibari.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object SnapshotManager {
    private val stateToTunationsMap = mutableMapOf<Any, MutableSet<Tunation>>()
    private val tunationToStatesMap = mutableMapOf<Tunation, MutableSet<Any>>()

    val tuneSnapshots = mutableSetOf<Snapshot>()

    init {
        GlobalSnapshotManager.ensureStarted()
        Snapshot.registerApplyObserver { stateObjects, snapshot ->
            if (snapshot !in tuneSnapshots) {
                val tunationsToInvalidate = mutableSetOf<Tunation>()
                stateObjects.forEach { stateObject ->
                    stateToTunationsMap[stateObject]?.let {
                        tunationsToInvalidate.addAll(it)
                    }
                }

                if (tunationsToInvalidate.isNotEmpty()) {
                    tunationsToInvalidate.forEach {
                        GlobalRetuner.retuner.scheduleRetune(it)
                    }
                }
            }
        }

    }


    fun recordRead(tunation: Tunation, stateObject: Any) {
        stateToTunationsMap.getOrPut(stateObject) { mutableSetOf() }.add(tunation)
        tunationToStatesMap.getOrPut(tunation) { mutableSetOf() }.add(stateObject)
    }

    fun clearDependencies(tunation: Tunation) {
        tunationToStatesMap[tunation]?.forEach { stateObject ->
            stateToTunationsMap[stateObject]?.remove(tunation)
        }
        tunationToStatesMap.remove(tunation)
    }
}

internal object GlobalSnapshotManager {
    private val started = AtomicBoolean(false)

    fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            val channel = Channel<Unit>(Channel.CONFLATED)
            val scope = CoroutineScope(Dispatchers.Main.immediate)

            scope.launch {
                channel.consumeEach {
                    Snapshot.sendApplyNotifications()
                }
            }

            Snapshot.registerGlobalWriteObserver {
                channel.trySend(Unit)
            }
        }
    }
}