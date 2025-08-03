package com.huanli233.hibari.runtime.effects

import android.util.Log
import com.huanli233.hibari.runtime.DisallowTunableCalls
import com.huanli233.hibari.runtime.RememberObserver
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.runtime.remember
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Tunable
fun LaunchedEffect(vararg keys: Any? = emptyArray(), block: suspend CoroutineScope.() -> Unit) {
    val coroutineScope = currentTuner.coroutineScope
    remember(*keys) { coroutineScope.launch(block = block) }
}

@PublishedApi
internal class CompositionScopedCoroutineScopeCanceller(
    val coroutineScope: CoroutineScope
) : RememberObserver {
    override fun onRemembered() {
        // Nothing to do
    }

    override fun onForgotten() {
        coroutineScope.cancel(CancellationException())
    }

    override fun onAbandoned() {
        coroutineScope.cancel(CancellationException())
    }
}

@Tunable
inline fun rememberCoroutineScope(
    crossinline getContext: @DisallowTunableCalls () -> CoroutineContext =
        { EmptyCoroutineContext }
): CoroutineScope {
    val tuner = currentTuner
    val wrapper = remember {
        CompositionScopedCoroutineScopeCanceller(
            CoroutineScope(tuner.coroutineScope.coroutineContext + Job(tuner.coroutineScope.coroutineContext[Job]) + getContext())
        )
    }
    return wrapper.coroutineScope
}
