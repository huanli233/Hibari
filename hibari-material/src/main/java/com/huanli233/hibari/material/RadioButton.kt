package com.huanli233.hibari.material

import com.google.android.material.radiobutton.MaterialRadioButton
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
fun RadioButton(
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    text: CharSequence? = null,
    textColor: Int? = null,
    textSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
) {
    Node(
        modifier = modifier
            .viewClass(MaterialRadioButton::class.java)
            .thenViewAttribute<MaterialRadioButton, Boolean>(checked) { isChecked ->
                this.isChecked = isChecked
            }
            .thenViewAttribute<MaterialRadioButton, ((Boolean) -> Unit)?>(onCheckedChange) { onChange ->
                setOnCheckedChangeListener { _, isChecked ->
                    onChange?.invoke(isChecked)
                }
            }
            .apply {
                if (text != null) {
                    text(text)
                }
                if (textColor != null) {
                    textColor(textColor)
                }
            }
            .textSize(textSize)
            .textAlign(textAlign)
    )
}