package com.huanli233.hibari.foundation.attributes

import android.view.View
import com.huanli233.hibari.foundation.layout.WindowInsets
import com.huanli233.hibari.foundation.layout.asPaddingValues
import com.huanli233.hibari.foundation.layout.navigationBars
import com.huanli233.hibari.foundation.layout.statusBars
import com.huanli233.hibari.foundation.layout.systemBars
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.locals.LocalDensity
import com.huanli233.hibari.runtime.locals.LocalLayoutDirection
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.unit.StablePaddingValues

@Tunable
fun Modifier.windowInsetsPadding(insets: WindowInsets): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val left = insets.getLeft(density, layoutDirection)
    val top = insets.getTop(density)
    val right = insets.getRight(density, layoutDirection)
    val bottom = insets.getBottom(density)
    return this.thenViewAttribute<View, Any>(uniqueKey, StablePaddingValues.fromPaddingValues(insets.asPaddingValues())) {
        this.setPadding(
            paddingLeft + left,
            paddingTop + top,
            paddingRight + right,
            paddingBottom + bottom
        )
    }
}

@Tunable
fun Modifier.statusBarsPadding(): Modifier = windowInsetsPadding(WindowInsets.statusBars)

@Tunable
fun Modifier.navigationBarsPadding(): Modifier = windowInsetsPadding(WindowInsets.navigationBars)

@Tunable
fun Modifier.systemBarsPadding(): Modifier =
    windowInsetsPadding(WindowInsets.systemBars)