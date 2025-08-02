package com.huanli233.hibari.foundation.attributes

import android.view.View
import android.view.ViewGroup
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenUnitLayoutAttribute
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.unit.Dp
import com.huanli233.hibari.ui.unit.DpSize
import com.huanli233.hibari.ui.unit.toPx

fun matchParentSize(): Modifier = Modifier.matchParentSize()
fun matchParentWidth(): Modifier = Modifier.matchParentWidth()
fun matchParentHeight(): Modifier = Modifier.matchParentHeight()

fun Modifier.matchParentWidth(): Modifier {
    return thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        width = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
fun Modifier.matchParentHeight(): Modifier {
    return thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        height = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
fun Modifier.matchParentSize(): Modifier {
    return thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
fun Modifier.width(width: Dp): Modifier {
    return this.thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        this.width = width.toPx(it)
    }
}
fun Modifier.height(height: Dp): Modifier {
    return this.thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        this.height = height.toPx(it)
    }
}
fun Modifier.size(size: DpSize): Modifier {
    return this.thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        this.width = size.width.toPx(it)
        this.height = size.height.toPx(it)
    }
}

fun Modifier.onClick(onClick: () -> Unit): Modifier {
    return this.thenViewAttribute<View, () -> Unit>(uniqueKey, onClick) {
        setOnClickListener { onClick() }
    }
}

fun Modifier.fitsSystemWindows(value: Boolean): Modifier {
    return this.thenViewAttribute<View, Boolean>(uniqueKey, value) { this.fitsSystemWindows = it }
}

fun Modifier.alpha(value: Float): Modifier {
    return this.thenViewAttribute<View, Float>(uniqueKey, value) { this.alpha = it }
}

fun Modifier.scaleX(value: Float): Modifier {
    return this.thenViewAttribute<View, Float>(uniqueKey, value) { this.scaleX = it }
}

fun Modifier.scaleY(value: Float): Modifier {
    return this.thenViewAttribute<View, Float>(uniqueKey, value) { this.scaleY = it }
}

fun Modifier.translationX(value: Float): Modifier {
    return this.thenViewAttribute<View, Float>(uniqueKey, value) { this.translationX = it }
}

fun Modifier.translationY(value: Float): Modifier {
    return this.thenViewAttribute<View, Float>(uniqueKey, value) { this.translationY = it }
}