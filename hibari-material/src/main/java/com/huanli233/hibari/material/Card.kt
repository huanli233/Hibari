package com.huanli233.hibari.material

import com.google.android.material.card.MaterialCardView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Float = 8f,
    elevation: Float = 4f,
    content: @Tunable () -> Unit,
) {
    Node(
        modifier = modifier
            .viewClass(MaterialCardView::class.java)
            .thenViewAttribute<MaterialCardView, Float>(cornerRadius) { radius ->
                radius = it
            }
            .thenViewAttribute<MaterialCardView, Float>(elevation) { elevation ->
                cardElevation = it
            }
            .content(content)
    )
}