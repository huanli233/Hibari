package com.huanli233.hibari.runtime

import com.huanli233.hibari.runtime.snapshots.Snapshot

object SnapshotManager {
    private val stateToTunationsMap = mutableMapOf<Any, MutableSet<Tunation>>()
    private val tunationToStatesMap = mutableMapOf<Tunation, MutableSet<Any>>()

    init {
        Snapshot.registerApplyObserver { stateObjects, snapshot ->
            val tunationsToInvalidate = mutableSetOf<Tunation>()
            stateObjects.forEach { stateObject ->
                stateToTunationsMap[stateObject]?.let {
                    tunationsToInvalidate.addAll(it)
                }
            }

            if (tunationsToInvalidate.isNotEmpty()) {
                tunationsToInvalidate.forEach {
                    GlobalRetuner.retuner.scheduleTune(it)
                }
            }
        }
        Snapshot.registerGlobalWriteObserver { n ->
            val tunationsToInvalidate = mutableSetOf<Tunation>()
            stateToTunationsMap[n]?.let {
                tunationsToInvalidate.addAll(it)
            }
            if (tunationsToInvalidate.isNotEmpty()) {
                tunationsToInvalidate.forEach {
                    GlobalRetuner.retuner.scheduleTune(it)
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