package com.huanli233.hibari.material

import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun ScrollView(modifier: Modifier = Modifier, content: @Tunable () -> Unit = {}) {
    Node(
        modifier = modifier
            .viewClass(ScrollView::class.java),
        content = content
    )
}

@Tunable
fun NestedScrollView(modifier: Modifier = Modifier, content: @Tunable () -> Unit = {}) {
    Node(
        modifier = modifier
            .viewClass(NestedScrollView::class.java),
        content = content
    )
}