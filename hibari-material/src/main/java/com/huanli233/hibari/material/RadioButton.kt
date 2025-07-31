package com.huanli233.hibari.material

import com.google.android.material.radiobutton.MaterialRadioButton
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.text
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun RadioButton(
    selected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true
) {
    Node(
        modifier = modifier
            .viewClass(MaterialRadioButton::class.java)
            .apply {
                if (text != null) {
                    this.text(text)
                }
                onClick { onSelectionChanged(!selected) }
            }
    )
}