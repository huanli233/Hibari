package com.huanli233.hibari.material

import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doOnTextChanged
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun EditText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: CharSequence? = null,
    inputType: Int? = null,
    maxLines: Int? = null,
) {
    Node(
        modifier = modifier
            .viewClass(AppCompatEditText::class.java)
            .thenViewAttribute<AppCompatEditText, String>(uniqueKey, value) {
                if (this.text.toString() != it) {
                    setText(it)
                    setSelection(it.length)
                }
            }
            .thenViewAttribute<AppCompatEditText, (String) -> Unit>(uniqueKey, onValueChange) { listener ->
                val currentValue = value
                doOnTextChanged { text, _, _, _ ->
                    val newText = text.toString()
                    if (newText != currentValue) {
                        listener(newText)
                    }
                }
            }
            .thenViewAttributeIfNotNull<AppCompatEditText, CharSequence>(uniqueKey, hint) { this.hint = it }
            .thenViewAttributeIfNotNull<AppCompatEditText, Int>(uniqueKey, inputType) { this.inputType = it }
            .thenViewAttributeIfNotNull<AppCompatEditText, Int>(uniqueKey, maxLines) { this.maxLines = it }
    )
}