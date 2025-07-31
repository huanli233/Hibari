package com.huanli233.hibari.material

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun LinearLayout(
    orientation: Int = LinearLayout.VERTICAL,
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit,
) {
    Node(
        modifier = modifier
            .viewClass(LinearLayout::class.java)
            .thenViewAttribute<LinearLayout, Int>(orientation) { orient ->
                this.orientation = orient
            }
            .content(content)
    )
}

@Tunable
fun FrameLayout(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit,
) {
    Node(
        modifier = modifier
            .viewClass(FrameLayout::class.java)
            .content(content)
    )
}

@Tunable
fun RelativeLayout(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit,
) {
    Node(
        modifier = modifier
            .viewClass(RelativeLayout::class.java)
            .content(content)
    )
}

@Tunable
fun ScrollView(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit,
) {
    Node(
        modifier = modifier
            .viewClass(android.widget.ScrollView::class.java)
            .content(content)
    )
}

@Tunable
fun HorizontalScrollView(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit,
) {
    Node(
        modifier = modifier
            .viewClass(android.widget.HorizontalScrollView::class.java)
            .content(content)
    )
}