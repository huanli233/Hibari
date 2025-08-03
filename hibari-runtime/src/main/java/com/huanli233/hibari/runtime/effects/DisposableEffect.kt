package com.huanli233.hibari.runtime.effects

import android.content.Context
import android.view.View
import com.huanli233.hibari.runtime.DisallowTunableCalls
import com.huanli233.hibari.runtime.RememberObserver
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.node.Node
import com.huanli233.hibari.ui.ref
import com.huanli233.hibari.ui.viewClass

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
    val impl = remember(*keys) {
        DisposableEffectImpl(effect)
    }
    Node(
        modifier = Modifier
            .viewClass(DisposableEffectView::class.java)
            .ref {
                it as DisposableEffectView
                it.onForgotten = impl::onForgotten
            }
    )
}

class DisposableEffectView(context: Context): View(context) {

    var onForgotten: () -> Unit = {}

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onForgotten()
    }

}