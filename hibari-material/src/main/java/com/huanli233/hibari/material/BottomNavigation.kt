package com.huanli233.hibari.material

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun BottomNavigation(
    modifier: Modifier = Modifier,
    selectedItemId: Int = 0,
    onItemSelected: (Int) -> Unit = {},
    content: @Tunable () -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(BottomNavigationView::class.java),
        content = content
    )
}