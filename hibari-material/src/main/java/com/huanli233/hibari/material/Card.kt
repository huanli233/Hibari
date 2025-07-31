package com.huanli233.hibari.material

import com.google.android.material.card.MaterialCardView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.foundation.attributes.padding
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.unit.Dp
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Card(
    modifier: Modifier = Modifier,
    contentPadding: Dp = Dp.Unspecified,
    onClick: (() -> Unit)? = null,
    content: @Tunable () -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(MaterialCardView::class.java)
            .apply {
                if (contentPadding != Dp.Unspecified) {
                    padding(contentPadding)
                }
                if (onClick != null) {
                    onClick(onClick)
                }
            },
        content = content
    )
}