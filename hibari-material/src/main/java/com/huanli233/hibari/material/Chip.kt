package com.huanli233.hibari.material

import com.google.android.material.chip.Chip
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.attributes.text
import com.huanli233.hibari.ui.attributes.textColor
import com.huanli233.hibari.ui.attributes.textSize
import com.huanli233.hibari.ui.text.TextAlign
import com.huanli233.hibari.ui.unit.TextUnit
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun Chip(
    text: CharSequence,
    modifier: Modifier = Modifier,
    textColor: Int? = null,
    textSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Center,
    onClick: (() -> Unit)? = null,
    closeIconVisible: Boolean = false,
    onCloseClick: (() -> Unit)? = null,
) {
    Node(
        modifier = modifier
            .viewClass(Chip::class.java)
            .text(text)
            .textSize(textSize)
            .textAlign(textAlign)
            .thenViewAttribute<Chip, (() -> Unit)?>(onClick) { clickListener ->
                setOnClickListener { clickListener?.invoke() }
            }
            .thenViewAttribute<Chip, Boolean>(closeIconVisible) { visible ->
                isCloseIconVisible = visible
            }
            .thenViewAttribute<Chip, (() -> Unit)?>(onCloseClick) { closeListener ->
                setOnCloseIconClickListener { closeListener?.invoke() }
            }
            .apply {
                if (textColor != null) {
                    textColor(textColor)
                }
            }
    )
}