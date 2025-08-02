package com.huanli233.hibari.runtime

import android.content.Context
import android.util.Log
import android.view.View
import com.huanli233.hibari.ui.uniqueKey

val currentTuner: Tuner
    @Tunable get() { throw NotImplementedError("Implemented as an intrinsic") }

val currentView: View
    @Tunable get() = currentTuner.tunation.hostView

val currentContext: Context
    @Tunable get() = currentTuner.tunation.hostView.context

@Tunable
inline fun <T> remember(vararg keys: Any? = emptyArray(), crossinline calculation: @DisallowTunableCalls () -> T): T =
    currentTuner.cache(keys, calculation)

@Tunable
inline fun <T> Tuner.cache(keys: Array<out Any?>, block: @DisallowTunableCalls () -> T): T {
    val remembered = rememberedValue()

    @Suppress("UNCHECKED_CAST")
    val entry = remembered as? Pair<Array<out Any?>, T>

    // TODO
    val key = currentTuner.walker.path() + "-${uniqueKey}"
    @Suppress("UNCHECKED_CAST")
    val invalid = entry == null || keys.contentEquals(currentTuner.memory[key] as? Array<Any> ?: emptyArray())
    currentTuner.memory[key] = keys

    return if (invalid) {
        (entry?.second as? RememberObserver)?.onForgotten()

        val value = block()
        updateRememberedValue(Pair(keys, value))

        (value as? RememberObserver)?.onRemembered()
        value
    } else {
        Log.d("huanli233", "return cached value")
        entry.second
    }
}

@Tunable
fun <T> key(
    @Suppress("UNUSED_PARAMETER")
    vararg keys: Any?,
    block: @Tunable () -> T
) = run {
    currentTuner.startGroup(keys.hashCode())
    val result = block()
    currentTuner.endGroup(keys.hashCode())
    result
}