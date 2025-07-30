package com.huanli233.hibari.runtime

import android.view.ViewGroup

object HibariApplier {

    fun applyTo(view: ViewGroup, content: @Tunable () -> Unit) {
        val tunation = Tunation(
            view,
            content = content
        )
        GlobalRecomposer.scheduleTune(tunation)
    }

}