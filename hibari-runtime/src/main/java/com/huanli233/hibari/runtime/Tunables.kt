package com.huanli233.hibari.runtime

import android.content.Context
import android.view.View

val currentTuner: Tuner
    @Tunable get() { throw NotImplementedError("Implemented as an intrinsic") }

val currentView: View
    @Tunable get() = currentTuner.tunation.hostView

val currentContext: Context
    @Tunable get() = currentTuner.tunation.hostView.context

@Tunable
inline fun <T> remember(vararg keys: Any? = emptyArray(), crossinline calculation: @DisallowTunableCalls () -> T): T =
    currentTuner.cache(keys, calculation)

inline fun <T> Tuner.cache(keys: Array<out Any?>, block: @DisallowTunableCalls () -> T): T {
    val remembered = rememberedValue()

    @Suppress("UNCHECKED_CAST")
    val entry = remembered as? Pair<Array<out Any?>, T>

    val invalid = entry == null || !entry.first.contentEquals(keys)

    return if (invalid) {
        (entry?.second as? RememberObserver)?.onForgotten()

        val value = block()
        updateRememberedValue(Pair(keys, value))

        (value as? RememberObserver)?.onRemembered()
        value
    } else {
        entry.second
    }
}