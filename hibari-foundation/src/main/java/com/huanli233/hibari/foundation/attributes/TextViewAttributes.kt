package com.huanli233.hibari.foundation.attributes

import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.widget.TextView
import com.huanli233.hibari.ui.AttributeApplier
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.ViewAttribute
import com.huanli233.hibari.ui.text.TextAlign
import com.huanli233.hibari.ui.text.TextTruncateAt
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.unit.TextUnit

fun Modifier.text(text: CharSequence): Modifier {
    return this.then(ViewAttribute(uniqueKey, object : AttributeApplier<TextView, CharSequence> {
        override fun apply(target: TextView, value: CharSequence) {
            target.text = value
        }
    }, text))
}

fun Modifier.textSize(textSize: TextUnit): Modifier {
    return this.then(ViewAttribute(uniqueKey, object : AttributeApplier<TextView, Float> {
        override fun apply(target: TextView, value: Float) {
            target.textSize = value
        }
    }, textSize.value))
}

fun Modifier.textColor(textColor: Int): Modifier {
    return this.then(ViewAttribute(uniqueKey, object : AttributeApplier<TextView, Int> {
        override fun apply(target: TextView, value: Int) {
            target.setTextColor(value)
        }
    }, textColor))
}

fun Modifier.textAlign(textAlign: TextAlign): Modifier {
    return this.then(ViewAttribute(uniqueKey, object : AttributeApplier<TextView, TextAlign> {
        override fun apply(target: TextView, value: TextAlign) {
            if (Build.VERSION.SDK_INT >= 17) {
                when (value) {
                    TextAlign.Left -> target.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
                    TextAlign.Right -> target.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
                    TextAlign.Center -> target.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    TextAlign.Start -> target.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
                    TextAlign.End -> target.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
                    TextAlign.Unspecified -> target.textAlignment = TextView.TEXT_ALIGNMENT_INHERIT
                    else -> target.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                }
            } else {
                when (value) {
                    TextAlign.Left -> target.gravity = Gravity.LEFT
                    TextAlign.Right -> target.gravity = Gravity.RIGHT
                    TextAlign.Center -> target.gravity = Gravity.CENTER_HORIZONTAL
                    TextAlign.Start -> target.gravity = Gravity.START
                    TextAlign.End -> target.gravity = Gravity.END
                    TextAlign.Unspecified -> target.gravity = Gravity.START
                    else -> target.gravity = Gravity.CENTER_HORIZONTAL
                }
            }
        }
    }, textAlign))
}

fun Modifier.minLines(minLines: Int): Modifier {
    return this.then(ViewAttribute(uniqueKey, object : AttributeApplier<TextView, Int> {
        override fun apply(target: TextView, value: Int) {
            target.minLines = value
        }
    }, minLines))
}

fun Modifier.maxLines(maxLines: Int): Modifier {
    return this.then(ViewAttribute(uniqueKey, object : AttributeApplier<TextView, Int> {
        override fun apply(target: TextView, value: Int) {
            target.maxLines = value
        }
    }, maxLines))
}

fun Modifier.ellipsis(ellipsis: TextTruncateAt): Modifier {
    return this.then(ViewAttribute(uniqueKey, object : AttributeApplier<TextView, TextTruncateAt> {
        override fun apply(target: TextView, value: TextTruncateAt) {
            when (value) {
                TextTruncateAt.START -> TextUtils.TruncateAt.START
                TextTruncateAt.MIDDLE -> TextUtils.TruncateAt.MIDDLE
                TextTruncateAt.END -> TextUtils.TruncateAt.END
                TextTruncateAt.MARQUEE -> TextUtils.TruncateAt.MARQUEE
            }
        }
    }, ellipsis))
}

fun Modifier.singleLine(singleLine: Boolean): Modifier {
    return this.thenViewAttribute<TextView, Boolean>(uniqueKey, singleLine) {
        isSingleLine = it
    }
}