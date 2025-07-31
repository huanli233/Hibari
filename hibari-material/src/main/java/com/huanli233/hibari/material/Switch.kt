package com.huanli233.hibari.material

import com.google.android.material.switchmaterial.SwitchMaterial
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Node(
        modifier = modifier
            .viewClass(SwitchMaterial::class.java)
            .onClick { onCheckedChange(!checked) }
    )
}