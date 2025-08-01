package com.huanli233.hibari.runtime

import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers

object HibariApplier {

    fun applyTo(
        view: ViewGroup,
        tuner: Tuner? = null,
        content: @Tunable () -> Unit
    ): Tunation {
        val tunation = Tunation(
            view,
            content = content,
            tuner = tuner
        )

        TuneController.tune(tunation)
        return tunation
    }
}

object GlobalRetuner {
    val retuner: Retuner by lazy { Retuner(Dispatchers.Main, emptyList()) }

    fun tuneNow(session: Tunation) {
        retuner.tuneNow(session)
    }
}