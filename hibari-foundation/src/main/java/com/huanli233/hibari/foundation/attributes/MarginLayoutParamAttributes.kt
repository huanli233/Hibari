package com.huanli233.hibari.foundation.attributes

import android.os.Build
import android.view.ViewGroup
import androidx.core.view.setMargins
import androidx.core.view.updateMargins
import androidx.core.view.updateMarginsRelative
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenLayoutAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.unit.Dp
import com.huanli233.hibari.ui.unit.toPx
import com.huanli233.hibari.ui.util.fastRoundToInt

fun Modifier.margins(margin: Dp): Modifier {
    return this.thenLayoutAttribute<ViewGroup.MarginLayoutParams, Dp>(uniqueKey, margin) { it, view ->
        setMargins(it.toPx(view))
    }
}

fun Modifier.margins(vertical: Dp = Dp.Unspecified, horizontal: Dp = Dp.Unspecified): Modifier {
    return this.thenLayoutAttribute<ViewGroup.MarginLayoutParams, Unit>(uniqueKey, Unit) { it, view ->
        val verticalPx = if (vertical != Dp.Unspecified) vertical.toPx(view) else this.topMargin
        val horizontalPx = if (horizontal != Dp.Unspecified) horizontal.toPx(view) else this.leftMargin

        updateMargins(
            left = horizontalPx,
            right = horizontalPx,
            top = verticalPx,
            bottom = verticalPx
        )
    }
}

fun Modifier.margins(left: Dp = Dp.Unspecified, top: Dp = Dp.Unspecified, right: Dp = Dp.Unspecified, bottom: Dp = Dp.Unspecified): Modifier {
    return this.thenLayoutAttribute<ViewGroup.MarginLayoutParams, MarginValues>(uniqueKey, MarginValues(top, bottom, left, right)) { it, view ->
        updateMargins(
            left = if (left != Dp.Unspecified) left.toPx(view) else this.leftMargin,
            right = if (right != Dp.Unspecified) right.toPx(view) else this.rightMargin,
            top = if (top != Dp.Unspecified) top.toPx(view) else this.topMargin,
            bottom = if (bottom != Dp.Unspecified) bottom.toPx(view) else this.bottomMargin
        )
    }
}

fun Modifier.marginRelative(start: Dp = Dp.Unspecified, top: Dp = Dp.Unspecified, end: Dp = Dp.Unspecified, bottom: Dp = Dp.Unspecified): Modifier {
    return this.thenLayoutAttribute<ViewGroup.MarginLayoutParams, MarginValues>(uniqueKey, MarginValues(top, bottom, start = start, end =  end)) { it, view ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            updateMarginsRelative(
                start = if (start != Dp.Unspecified) start.toPx(view) else this.marginStart,
                top = if (top != Dp.Unspecified) top.toPx(view) else this.topMargin,
                end = if (end != Dp.Unspecified) end.toPx(view) else this.marginEnd,
                bottom = if (bottom != Dp.Unspecified) bottom.toPx(view) else this.bottomMargin
            )
        } else {
            updateMargins(
                left = if (start != Dp.Unspecified) start.toPx(view) else this.leftMargin,
                top = if (top != Dp.Unspecified) top.toPx(view) else this.topMargin,
                right = if (end != Dp.Unspecified) end.toPx(view) else this.rightMargin,
                bottom = if (bottom != Dp.Unspecified) bottom.toPx(view) else this.bottomMargin
            )
        }
    }
}

fun Modifier.marginRelative(marginValues: MarginValues): Modifier {
    return this.thenLayoutAttribute<ViewGroup.MarginLayoutParams, MarginValues>(uniqueKey, marginValues) { pv, view ->
        val topPx = if (pv.top != Dp.Unspecified) pv.top.toPx(view) else this.topMargin
        val bottomPx = if (pv.bottom != Dp.Unspecified) pv.bottom.toPx(view) else this.bottomMargin

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val startPx = pv.start?.takeIf { it != Dp.Unspecified }?.value?.fastRoundToInt() ?: this.marginStart
            val endPx = pv.end?.takeIf { it != Dp.Unspecified }?.value?.fastRoundToInt() ?: this.marginEnd
            updateMarginsRelative(
                start = startPx,
                top = topPx,
                end = endPx,
                bottom = bottomPx
            )
        } else {
            val leftPx = pv.start?.takeIf { it != Dp.Unspecified }?.value?.fastRoundToInt() ?: this.leftMargin
            val rightPx = pv.end?.takeIf { it != Dp.Unspecified }?.value?.fastRoundToInt() ?: this.rightMargin
            updateMargins(
                left = leftPx,
                top = topPx,
                right = rightPx,
                bottom = bottomPx
            )
        }
    }
}

data class MarginValues(
    val top: Dp = Dp.Unspecified,
    val bottom: Dp = Dp.Unspecified,
    val left: Dp = Dp.Unspecified,
    val right: Dp = Dp.Unspecified,
    val start: Dp? = null,
    val end: Dp? = null,
)