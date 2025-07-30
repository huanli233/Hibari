package com.huanli233.hibari.runtime.effects

import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Tunable
fun LaunchedEffect(block: suspend CoroutineScope.() -> Unit) {
    val coroutineScope = currentTuner.coroutineScope
    remember { coroutineScope.launch(block = block) }
}