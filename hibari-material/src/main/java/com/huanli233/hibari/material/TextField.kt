package com.huanli233.hibari.material

import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.text
import com.huanli233.hibari.foundation.attributes.textSize
import com.huanli233.hibari.foundation.attributes.textColor
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.unit.TextUnit
import com.huanli233.hibari.ui.viewClass

@Tunable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    textSize: TextUnit = TextUnit.Unspecified,
    textColor: Int? = null,
    onClick: (() -> Unit)? = null
) {
    Node(
        modifier = modifier
            .viewClass(TextInputLayout::class.java)
            .apply {
                if (label != null) {
                    // TODO: Add label attribute support
                }
            },
        content = {
            Node(
                modifier = Modifier
                    .viewClass(TextInputEditText::class.java)
                    .text(value)
                    .apply {
                        if (textSize != TextUnit.Unspecified) {
                            textSize(textSize)
                        }
                        if (textColor != null) {
                            textColor(textColor)
                        }
                        if (onClick != null) {
                            onClick(onClick)
                        }
                    }
            )
        }
    )
}