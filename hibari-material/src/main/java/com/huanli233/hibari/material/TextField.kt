package com.huanli233.hibari.material

import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.attributes.hint
import com.huanli233.hibari.ui.attributes.text
import com.huanli233.hibari.ui.attributes.textColor
import com.huanli233.hibari.ui.attributes.textSize
import com.huanli233.hibari.ui.text.TextAlign
import com.huanli233.hibari.ui.unit.TextUnit
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun TextField(
    value: String = "",
    onValueChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    hint: CharSequence? = null,
    textColor: Int? = null,
    textSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    singleLine: Boolean = true,
) {
    Node(
        modifier = modifier
            .viewClass(TextInputLayout::class.java)
            .thenViewAttribute<TextInputLayout, String>(value) { text ->
                editText?.setText(text)
            }
            .thenViewAttribute<TextInputLayout, ((String) -> Unit)?>(onValueChange) { onChange ->
                editText?.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        onChange?.invoke(s?.toString() ?: "")
                    }
                })
            }
            .apply {
                if (hint != null) {
                    hint(hint)
                }
            }
            .content {
                Node(
                    modifier = Modifier
                        .viewClass(TextInputEditText::class.java)
                        .text(value)
                        .textSize(textSize)
                        .textAlign(textAlign)
                        .singleLine(singleLine)
                        .apply {
                            if (textColor != null) {
                                textColor(textColor)
                            }
                        }
                )
            }
    )
}