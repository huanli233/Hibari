package com.huanli233.hibari.sample

import com.huanli233.hibari.runtime.MutableState
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.snapshots.Snapshot

class Dog {
    var name: MutableState<String> = mutableStateOf("")
}

fun main() {
    val dog = Dog()
    dog.name.value = "Spot"
    val snapshot = Snapshot.takeMutableSnapshot(
        readObserver = {
            println("Read! $it")
        },
        writeObserver = {
            println("Write! $it")
        }
    )
    Snapshot.registerGlobalWriteObserver {
        println("Global write! $it")
    }
    Snapshot.registerApplyObserver { set, snapshot ->
        println("Apply! ${set.joinToString()} $snapshot")
    }
    println("Value1: ${dog.name.value}")
    snapshot.enter {
        dog.name.value = "Fido"
        println("Value2(Writed in snapshot): ${dog.name.value}")
    }
    println("Value3(Snapshot exited): ${dog.name.value}")
    snapshot.apply()
    println("Value4(Snapshot applied): ${dog.name.value}")
}