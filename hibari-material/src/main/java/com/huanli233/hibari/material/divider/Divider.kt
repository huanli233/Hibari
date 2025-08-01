package com.huanli233.hibari.material.divider

import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.google.android.material.divider.MaterialDivider
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Divider(
    modifier: Modifier = Modifier,
    @ColorInt color: Int? = null,
    @Px thickness: Int? = null,
    @Px insetStart: Int? = null,
    @Px insetEnd: Int? = null
) {
    Node(
        modifier = modifier
            .viewClass(MaterialDivider::class.java)
            .thenViewAttributeIfNotNull<MaterialDivider, Int>(uniqueKey, color) { dividerColor = it }
            .thenViewAttributeIfNotNull<MaterialDivider, Int>(uniqueKey, thickness) { dividerThickness = it }
            .thenViewAttributeIfNotNull<MaterialDivider, Int>(uniqueKey, insetStart) { dividerInsetStart = it }
            .thenViewAttributeIfNotNull<MaterialDivider, Int>(uniqueKey, insetEnd) { dividerInsetEnd = it }
    )
}