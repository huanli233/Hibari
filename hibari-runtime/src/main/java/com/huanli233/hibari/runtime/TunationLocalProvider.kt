package com.huanli233.hibari.runtime

@Tunable
fun TunationLocalProvider(
    vararg value: ProvidedValue<*>,
    content: @Tunable () -> Unit
) {
    val tuner = currentTuner
    tuner.startProviders(value)
    tuner.runTunable(content)
    tuner.endProviders(value)
}