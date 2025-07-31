package com.huanli233.hibari.material

import com.google.android.material.chip.Chip
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.text
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false
) {
    Node(
        modifier = modifier
            .viewClass(Chip::class.java)
            .text(text)
            .apply {
                if (onClick != null) {
                    onClick(onClick)
                }
            }
    )
}