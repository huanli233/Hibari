package com.huanli233.hibari.runtime

val currentTuner: Tuner
    @Tunable get() { throw NotImplementedError("Implemented as an intrinsic") }

@Tunable
inline fun <T> remember(crossinline calculation: @DisallowTunableCalls () -> T): T =
    currentTuner.cache(false, calculation)

inline fun <T> Tuner.cache(invalid: Boolean, block: @DisallowTunableCalls () -> T): T {
    @Suppress("UNCHECKED_CAST")
    return rememberedValue().let {
        if (invalid || it == null) {
            val value = block()
            updateRememberedValue(value)
            value
        } else it
    } as T
}