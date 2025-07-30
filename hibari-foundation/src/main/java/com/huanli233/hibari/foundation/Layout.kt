package com.huanli233.hibari.foundation

import android.view.ViewGroup
import com.huanli233.hibari.runtime.HibariApplier
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute

fun <V : ViewGroup, T> Modifier.content(
    content: @Tunable () -> Unit
) = this.thenViewAttribute<ViewGroup, @Tunable () -> Unit>(content) {
    removeAllViews()
    HibariApplier.applyTo(this, it)
}