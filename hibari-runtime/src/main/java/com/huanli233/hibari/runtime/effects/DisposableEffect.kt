package com.huanli233.hibari.runtime.effects

import com.huanli233.hibari.runtime.DisallowTunableCalls
import com.huanli233.hibari.runtime.RememberObserver
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.remember

typealias OnDispose = () -> Unit

private class DisposableEffectImpl(
    private val effect: () -> OnDispose
) : RememberObserver {
    private var onDispose: OnDispose? = null

    override fun onRemembered() {
        onDispose = effect()
    }

    override fun onForgotten() {
        onDispose?.invoke()
        onDispose = null
    }
}

@Tunable
fun DisposableEffect(vararg keys: Any?, effect: @DisallowTunableCalls () -> OnDispose) {
    remember(*keys) {
        DisposableEffectImpl(effect)
    }
}