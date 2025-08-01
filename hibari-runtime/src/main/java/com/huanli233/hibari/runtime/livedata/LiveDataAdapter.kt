package com.huanli233.hibari.runtime.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.effects.DisposableEffect
import com.huanli233.hibari.runtime.locals.LocalLifecycleOwner
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.remember

@Tunable
fun <T> LiveData<T>.observeAsState(): com.huanli233.hibari.runtime.State<T?> = observeAsState(value)

@Tunable
fun <R, T : R> LiveData<T>.observeAsState(initial: R): com.huanli233.hibari.runtime.State<R> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember {
        @Suppress("UNCHECKED_CAST") /* Initialized values of a LiveData<T> must be a T */
        mutableStateOf(if (isInitialized) value as T else initial)
    }
    DisposableEffect(this, lifecycleOwner) {
        val observer = Observer<T> { state.value = it }
        observe(lifecycleOwner, observer);
        {
            removeObserver(observer)
        }
    }
    return state
}