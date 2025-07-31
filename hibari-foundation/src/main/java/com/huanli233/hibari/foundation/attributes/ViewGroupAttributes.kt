package com.huanli233.hibari.foundation.attributes

import android.view.ViewGroup
import com.huanli233.hibari.runtime.invokeSetKeyedTag
import com.huanli233.hibari.runtime.subcomposeLayoutId
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenUnitViewAttribute
import com.huanli233.hibari.ui.uniqueKey

fun Modifier.subcompose(): Modifier = this.thenUnitViewAttribute<ViewGroup>(uniqueKey) {
    invokeSetKeyedTag(this, subcomposeLayoutId, true)
}