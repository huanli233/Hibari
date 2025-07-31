package com.huanli233.hibari.material

import com.google.android.material.checkbox.MaterialCheckBox
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.text
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true
) {
    Node(
        modifier = modifier
            .viewClass(MaterialCheckBox::class.java)
            .apply {
                if (text != null) {
                    this.text(text)
                }
                onClick { onCheckedChange(!checked) }
            }
    )
}