package com.huanli233.hibari.material

import com.google.android.material.tabs.TabLayout
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun TabRow(
    selectedTabIndex: Int = 0,
    modifier: Modifier = Modifier,
    onTabSelected: (Int) -> Unit = {},
    content: @Tunable () -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(TabLayout::class.java),
        content = content
    )
}