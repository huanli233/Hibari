package com.huanli233.hibari.material.flow

import com.google.android.material.internal.FlowLayout
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun FlowLayout(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit
) {
    Node(modifier = modifier.viewClass(FlowLayout::class.java), content = content)
}