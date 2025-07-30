package com.huanli233.hibari.runtime

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.text
import com.huanli233.hibari.ui.viewClass

object HibariApplier {

    fun applyTo(view: ViewGroup, content: @Tunable () -> Unit) {
        val tunation = Tunation(
            view,
            content = content
        )
        GlobalRecomposer.scheduleTune(tunation)
    }

}