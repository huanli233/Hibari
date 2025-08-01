package com.huanli233.hibari.foundation.attributes

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenUnitLayoutAttribute
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.unit.Dp
import com.huanli233.hibari.ui.unit.DpSize
import com.huanli233.hibari.ui.unit.toPx
import com.huanli233.hibari.ui.util.fastRoundToInt

fun Modifier.fillMaxWidth(): Modifier {
    return thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        width = ViewGroup.LayoutParams.MATCH_PARENT
    }
}

fun Modifier.fillMaxHeight(): Modifier {
    return thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        height = ViewGroup.LayoutParams.MATCH_PARENT
    }
}

fun Modifier.fillMaxSize(): Modifier {
    return thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
    }
}

@Tunable
fun Modifier.width(width: Dp): Modifier {
    return this.thenUnitLayoutAttribute<ViewGroup.LayoutParams>(uniqueKey) {
        this.width = width.toPx(it)
    }
}

@Tunable
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

fun Modifier.padding(padding: Dp): Modifier {
    return this.thenViewAttribute<View, Dp>(uniqueKey, padding) {
        setPadding(it.toPx(this))
    }
}

fun Modifier.padding(vertical: Dp = Dp.Unspecified, horizontal: Dp = Dp.Unspecified): Modifier {
    return this.thenViewAttribute<View, Unit>(uniqueKey, Unit) {
        val verticalPx = if (vertical != Dp.Unspecified) vertical.toPx(this) else this.paddingTop
        val horizontalPx = if (horizontal != Dp.Unspecified) horizontal.toPx(this) else this.paddingLeft

        updatePadding(
            left = horizontalPx,
            right = horizontalPx,
            top = verticalPx,
            bottom = verticalPx
        )
    }
}

fun Modifier.padding(left: Dp = Dp.Unspecified, top: Dp = Dp.Unspecified, right: Dp = Dp.Unspecified, bottom: Dp = Dp.Unspecified): Modifier {
    return this.thenViewAttribute<View, PaddingValues>(uniqueKey, PaddingValues(top, bottom, left, right)) {
        updatePadding(
            left = if (left != Dp.Unspecified) left.toPx(this) else this.paddingLeft,
            right = if (right != Dp.Unspecified) right.toPx(this) else this.paddingRight,
            top = if (top != Dp.Unspecified) top.toPx(this) else this.paddingTop,
            bottom = if (bottom != Dp.Unspecified) bottom.toPx(this) else this.paddingBottom
        )
    }
}

fun Modifier.paddingRelative(start: Dp = Dp.Unspecified, top: Dp = Dp.Unspecified, end: Dp = Dp.Unspecified, bottom: Dp = Dp.Unspecified): Modifier {
    return this.thenViewAttribute<View, PaddingValues>(uniqueKey, PaddingValues(top, bottom, start = start, end =  end)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            updatePaddingRelative(
                start = if (start != Dp.Unspecified) start.toPx(this) else this.paddingStart,
                top = if (top != Dp.Unspecified) top.toPx(this) else this.paddingTop,
                end = if (end != Dp.Unspecified) end.toPx(this) else this.paddingEnd,
                bottom = if (bottom != Dp.Unspecified) bottom.toPx(this) else this.paddingBottom
            )
        } else {
            updatePadding(
                left = if (start != Dp.Unspecified) start.toPx(this) else this.paddingLeft,
                top = if (top != Dp.Unspecified) top.toPx(this) else this.paddingTop,
                right = if (end != Dp.Unspecified) end.toPx(this) else this.paddingRight,
                bottom = if (bottom != Dp.Unspecified) bottom.toPx(this) else this.paddingBottom
            )
        }
    }
}

fun Modifier.paddingRelative(paddingValues: PaddingValues): Modifier {
    return this.thenViewAttribute<View, PaddingValues>(uniqueKey, paddingValues) { pv ->
        val topPx = if (pv.top != Dp.Unspecified) pv.top.toPx(this) else this.paddingTop
        val bottomPx = if (pv.bottom != Dp.Unspecified) pv.bottom.toPx(this) else this.paddingBottom

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val startPx = pv.start?.takeIf { it != Dp.Unspecified }?.value?.fastRoundToInt() ?: this.paddingStart
            val endPx = pv.end?.takeIf { it != Dp.Unspecified }?.value?.fastRoundToInt() ?: this.paddingEnd
            updatePaddingRelative(
                start = startPx,
                top = topPx,
                end = endPx,
                bottom = bottomPx
            )
        } else {
            val leftPx = pv.start?.takeIf { it != Dp.Unspecified }?.value?.fastRoundToInt() ?: this.paddingLeft
            val rightPx = pv.end?.takeIf { it != Dp.Unspecified }?.value?.fastRoundToInt() ?: this.paddingRight
            updatePadding(
                left = leftPx,
                top = topPx,
                right = rightPx,
                bottom = bottomPx
            )
        }
    }
}

data class PaddingValues(
    val top: Dp = Dp.Unspecified,
    val bottom: Dp = Dp.Unspecified,
    val left: Dp = Dp.Unspecified,
    val right: Dp = Dp.Unspecified,
    val start: Dp? = null,
    val end: Dp? = null,
)

fun Modifier.onClick(onClick: () -> Unit): Modifier {
    return this.thenViewAttribute<View, () -> Unit>(uniqueKey, onClick) {
        setOnClickListener { onClick() }
    }
}

fun Modifier.fitsSystemWindows(value: Boolean): Modifier {
    return this.thenViewAttribute<View, Boolean>(uniqueKey, value) { this.fitsSystemWindows = it }
}