package com.huanli233.hibari.foundation.attributes

import android.os.Build
import android.view.View
import android.view.ViewGroup
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenUnitLayoutAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.unit.Dp
import com.huanli233.hibari.ui.unit.LayoutDirection
import com.huanli233.hibari.ui.unit.PaddingValues
import com.huanli233.hibari.ui.unit.toPx

fun Modifier.margin(marginValues: PaddingValues): Modifier {
    return this.thenUnitLayoutAttribute<ViewGroup.MarginLayoutParams>(uniqueKey) { view ->
        val layoutDirection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            when (view.layoutDirection) {
                View.LAYOUT_DIRECTION_LTR -> LayoutDirection.Ltr
                View.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
                else -> LayoutDirection.Ltr
            }
        } else {
            LayoutDirection.Ltr
        }

        leftMargin = marginValues.calculateLeftPadding(layoutDirection).toPx(view)
        rightMargin = marginValues.calculateRightPadding(layoutDirection).toPx(view)
        topMargin = marginValues.calculateTopPadding().toPx(view)
        bottomMargin = marginValues.calculateBottomPadding().toPx(view)
    }
}

fun Modifier.margin(all: Dp): Modifier {
    return margin(PaddingValues(all = all))
}

fun Modifier.margin(vertical: Dp = Dp.Unspecified, horizontal: Dp = Dp.Unspecified): Modifier {
    return margin(PaddingValues(vertical = vertical, horizontal = horizontal))
}

fun Modifier.margin(
    left: Dp = Dp.Unspecified,
    top: Dp = Dp.Unspecified,
    right: Dp = Dp.Unspecified,
    bottom: Dp = Dp.Unspecified
): Modifier {
    return margin(PaddingValues(start = left, top = top, end = right, bottom = bottom))
}

fun Modifier.marginRelative(
    start: Dp = Dp.Unspecified,
    top: Dp = Dp.Unspecified,
    end: Dp = Dp.Unspecified,
    bottom: Dp = Dp.Unspecified
): Modifier {
    return margin(PaddingValues(start = start, top = top, end = end, bottom = bottom))
}