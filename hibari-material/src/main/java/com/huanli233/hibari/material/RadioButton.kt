package com.huanli233.hibari.material

import com.google.android.material.radiobutton.MaterialRadioButton
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: CharSequence? = null,
    enabled: Boolean? = null
) {
    Node(
        modifier = modifier
            .viewClass(MaterialRadioButton::class.java)
            .thenViewAttribute<MaterialRadioButton, Boolean>(uniqueKey, selected) {
                if (this.isChecked != it) {
                    this.isChecked = it
                }
            }
            .thenViewAttribute<MaterialRadioButton, () -> Unit>(uniqueKey, onClick) { listener ->
                setOnClickListener { listener() }
            }
            .thenViewAttributeIfNotNull<MaterialRadioButton, CharSequence>(uniqueKey, text) { this.text = it }
            .thenViewAttributeIfNotNull<MaterialRadioButton, Boolean>(uniqueKey, enabled) { this.isEnabled = it }
    )
}