package com.huanli233.hibari.material

import com.google.android.material.button.MaterialButton
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.attributes.text
import com.huanli233.hibari.ui.attributes.textColor
import com.huanli233.hibari.ui.attributes.textSize
import com.huanli233.hibari.ui.text.TextAlign
import com.huanli233.hibari.ui.unit.TextUnit
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Button(
    text: CharSequence,
    modifier: Modifier = Modifier,
    textColor: Int? = null,
    textSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Center,
    onClick: (() -> Unit)? = null,
) {
    Node(
        modifier = modifier
            .viewClass(MaterialButton::class.java)
            .text(text)
            .textSize(textSize)
            .textAlign(textAlign)
            .apply {
                if (textColor != null) {
                    textColor(textColor)
                }
                if (onClick != null) {
                    thenViewAttribute<MaterialButton, (() -> Unit)?>(onClick) { clickListener ->
                        setOnClickListener { clickListener?.invoke() }
                    }
                }
            }
    )
}