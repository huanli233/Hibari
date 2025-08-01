package com.huanli233.hibari.material

import com.google.android.material.checkbox.MaterialCheckBox
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun CheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    text: CharSequence? = null,
    enabled: Boolean? = null
) {
    Node(
        modifier = modifier
            .viewClass(MaterialCheckBox::class.java)
            .thenViewAttribute<MaterialCheckBox, Boolean>(uniqueKey, checked) {
                if (this.isChecked != it) {
                    this.isChecked = it
                }
            }
            .thenViewAttribute<MaterialCheckBox, (Boolean) -> Unit>(uniqueKey, onCheckedChange) { listener ->
                setOnCheckedChangeListener { _, isChecked -> listener(isChecked) }
            }
            .thenViewAttributeIfNotNull<MaterialCheckBox, CharSequence>(uniqueKey, text) { this.text = it }
            .thenViewAttributeIfNotNull<MaterialCheckBox, Boolean>(uniqueKey, enabled) { this.isEnabled = it }
    )
}