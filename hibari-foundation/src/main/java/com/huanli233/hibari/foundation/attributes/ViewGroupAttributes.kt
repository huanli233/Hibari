package com.huanli233.hibari.foundation.attributes

import android.view.View
import android.view.ViewGroup
import com.huanli233.hibari.runtime.invokeSetKeyedTag
import com.huanli233.hibari.runtime.subcomposeLayoutId
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenUnitViewAttribute
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.uniqueKey

fun Modifier.subcompose(): Modifier = this.thenUnitViewAttribute<ViewGroup>(uniqueKey) {
    invokeSetKeyedTag(this, subcomposeLayoutId, true)
}

fun Modifier.clipToPadding(value: Boolean): Modifier {
    return this.thenViewAttribute<ViewGroup, Boolean>(uniqueKey, value) { this.clipToPadding = it }
}