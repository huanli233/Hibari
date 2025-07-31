package com.huanli233.hibari.material

import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun TextField(
    value: String = "",
    onValueChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    errorMessage: String? = null,
    helperText: String? = null,
    inputType: Int? = null,
    @TextInputLayout.EndIconMode endIconMode: Int? = null,
) {
    TextInputLayout(
        modifier = modifier,
        hint = label,
        errorMessage = errorMessage,
        helperText = helperText,
        endIconMode = endIconMode
    ) {
        TextInputEditText(
            value = value,
            onValueChange = onValueChange,
            hint = placeholder,
            inputType = inputType
        )
    }
}

@Tunable
fun TextInputLayout(
    modifier: Modifier = Modifier,
    hint: CharSequence? = null,
    helperText: CharSequence? = null,
    errorMessage: CharSequence? = null,
    counterEnabled: Boolean? = null,
    counterMaxLength: Int? = null,
    @TextInputLayout.EndIconMode endIconMode: Int? = null,
    content: @Tunable () -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(TextInputLayout::class.java)
            .thenViewAttributeIfNotNull<TextInputLayout, CharSequence>(uniqueKey, hint) { this.hint = it }
            .thenViewAttributeIfNotNull<TextInputLayout, CharSequence>(uniqueKey, helperText) { this.helperText = it }
            .thenViewAttributeIfNotNull<TextInputLayout, CharSequence>(uniqueKey, errorMessage) {
                this.isErrorEnabled = true
                this.error = it
            }
            .thenViewAttributeIfNotNull<TextInputLayout, Boolean>(uniqueKey, counterEnabled) { this.isCounterEnabled = it }
            .thenViewAttributeIfNotNull<TextInputLayout, Int>(uniqueKey, counterMaxLength) { this.counterMaxLength = it }
            .thenViewAttributeIfNotNull<TextInputLayout, Int>(uniqueKey, endIconMode) { this.endIconMode = it },
        content = content
    )
}

@Tunable
private fun TextInputEditText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: CharSequence? = null,
    inputType: Int? = null
) {
    Node(
        modifier = modifier
            .viewClass(TextInputEditText::class.java)
            .thenViewAttribute<TextInputEditText, String>(uniqueKey, value) {
                if (this.text.toString() != it) {
                    setText(it)
                    setSelection(it.length)
                }
            }
            .thenViewAttribute<TextInputEditText, (String) -> Unit>(uniqueKey, onValueChange) { listener ->
                val currentValue = value
                doOnTextChanged { text, _, _, _ ->
                    val newText = text.toString()
                    if (newText != currentValue) {
                        listener(newText)
                    }
                }
            }
            .thenViewAttributeIfNotNull<TextInputEditText, CharSequence>(uniqueKey, hint) { this.hint = it }
            .thenViewAttributeIfNotNull<TextInputEditText, Int>(uniqueKey, inputType) { this.inputType = it }
    )
}