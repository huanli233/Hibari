package com.huanli233.hibari.runtime

import android.view.ViewGroup

object HibariApplier {

    fun applyTo(
        view: ViewGroup,
        tuner: Tuner? = null,
        content: @Tunable () -> Unit
    ) {
        val tunation = Tunation(
            view,
            content = content,
            tuner = tuner
        )
        GlobalRetuner.scheduleTune(tunation)
    }

}