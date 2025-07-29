package com.huanli233.hibari.sample

import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.Tuner
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.runtime.remember
import java.util.UUID

@Tunable
fun topLevelTunable(message: String) {
    println("Inside topLevelTunable: $message")
    anotherTunable()
}

@Tunable
fun anotherTunable() {
    println("Inside anotherTunable")
}

class MyTest {
    @Tunable
    fun classMethodTunable() {
        println("Inside classMethodTunable")
        anotherTunable()
    }
}

@Tunable
fun highOrderFunction(block: @Tunable () -> Unit) {
    println("Before calling tunable lambda")
    block()
    println("After calling tunable lambda")
}

fun nonTunableCaller() {
    println("This function is not tunable.")
//    anotherTunable()
}

fun main() {
    val tuner = Tuner()
    val test = MyTest()

    val tunable = @Tunable {
        val value = remember { UUID.randomUUID().toString() }
        println("UUID: $value")
    }

    tuner.runTunable(tunable)
    tuner.runTunable(tunable)
}
