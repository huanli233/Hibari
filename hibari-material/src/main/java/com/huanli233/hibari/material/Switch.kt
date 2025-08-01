package com.huanli233.hibari.material

import com.google.android.material.materialswitch.MaterialSwitch
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Switch(
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    text: CharSequence? = null,
    enabled: Boolean? = null
) {
    Node(
        modifier = modifier
            .viewClass(MaterialSwitch::class.java)
            .thenViewAttribute<MaterialSwitch, Boolean>(uniqueKey, checked) {
                if (this.isChecked != it) {
                    this.isChecked = it
                }
            }
            .thenViewAttribute<MaterialSwitch, (Boolean) -> Unit>(uniqueKey, onCheckedChange) { listener ->
                setOnCheckedChangeListener { _, isChecked -> listener(isChecked) }
            }
            .thenViewAttributeIfNotNull<MaterialSwitch, CharSequence>(uniqueKey, text) { this.text = it }
            .thenViewAttributeIfNotNull<MaterialSwitch, Boolean>(uniqueKey, enabled) { this.isEnabled = it }
    )
}