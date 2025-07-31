package com.huanli233.hibari.ui.attributes

import android.view.View
import com.huanli233.hibari.ui.AttributeApplier
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.ViewAttribute
import com.huanli233.hibari.ui.thenViewAttribute

fun Modifier.background(background: Int): Modifier {
    return this.then(ViewAttribute(object : AttributeApplier<View, Int> {
        override fun apply(target: View, value: Int) {
            target.setBackgroundResource(value)
        }
    }, background))
}

fun Modifier.backgroundColor(color: Int): Modifier {
    return this.then(ViewAttribute(object : AttributeApplier<View, Int> {
        override fun apply(target: View, value: Int) {
            target.setBackgroundColor(value)
        }
    }, color))
}

fun Modifier.alpha(alpha: Float): Modifier {
    return this.then(ViewAttribute(object : AttributeApplier<View, Float> {
        override fun apply(target: View, value: Float) {
            target.alpha = value
        }
    }, alpha))
}

fun Modifier.visibility(visibility: Int): Modifier {
    return this.then(ViewAttribute(object : AttributeApplier<View, Int> {
        override fun apply(target: View, value: Int) {
            target.visibility = value
        }
    }, visibility))
}

fun Modifier.enabled(enabled: Boolean): Modifier {
    return this.then(ViewAttribute(object : AttributeApplier<View, Boolean> {
        override fun apply(target: View, value: Boolean) {
            target.isEnabled = value
        }
    }, enabled))
}

fun Modifier.clickable(clickable: Boolean): Modifier {
    return this.then(ViewAttribute(object : AttributeApplier<View, Boolean> {
        override fun apply(target: View, value: Boolean) {
            target.isClickable = value
        }
    }, clickable))
}

fun Modifier.focusable(focusable: Boolean): Modifier {
    return this.then(ViewAttribute(object : AttributeApplier<View, Boolean> {
        override fun apply(target: View, value: Boolean) {
            target.isFocusable = value
        }
    }, focusable))
}

fun Modifier.padding(padding: Int): Modifier {
    return this.then(ViewAttribute(object : AttributeApplier<View, Int> {
        override fun apply(target: View, value: Int) {
            target.setPadding(value, value, value, value)
        }
    }, padding))
}

fun Modifier.paddingHorizontal(horizontal: Int): Modifier {
    return this.thenViewAttribute<View, Int>(horizontal) { padding ->
        setPadding(padding, paddingTop, padding, paddingBottom)
    }
}

fun Modifier.paddingVertical(vertical: Int): Modifier {
    return this.thenViewAttribute<View, Int>(vertical) { padding ->
        setPadding(paddingLeft, padding, paddingRight, padding)
    }
}

fun Modifier.margin(margin: Int): Modifier {
    return this.thenViewAttribute<View, Int>(margin) { marginValue ->
        val params = layoutParams
        if (params != null) {
            params.leftMargin = marginValue
            params.topMargin = marginValue
            params.rightMargin = marginValue
            params.bottomMargin = marginValue
            layoutParams = params
        }
    }
}