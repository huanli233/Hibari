package com.huanli233.hibari.material.circularreveal

import com.google.android.material.circularreveal.CircularRevealFrameLayout
import com.google.android.material.circularreveal.CircularRevealGridLayout
import com.google.android.material.circularreveal.CircularRevealLinearLayout
import com.google.android.material.circularreveal.CircularRevealRelativeLayout
import com.google.android.material.circularreveal.cardview.CircularRevealCardView
import com.google.android.material.circularreveal.coordinatorlayout.CircularRevealCoordinatorLayout
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.material.CardScope
import com.huanli233.hibari.material.CardScopeInstance
import com.huanli233.hibari.material.coordinatorlayout.CoordinatorLayoutScope
import com.huanli233.hibari.material.coordinatorlayout.CoordinatorLayoutScopeInstance
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun CircularRevealFrameLayout(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit
) {
    Node(modifier.viewClass(CircularRevealFrameLayout::class.java), content)
}

@Tunable
fun CircularRevealLinearLayout(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit
) {
    Node(modifier.viewClass(CircularRevealLinearLayout::class.java), content)
}

@Tunable
fun CircularRevealRelativeLayout(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit
) {
    Node(modifier.viewClass(CircularRevealRelativeLayout::class.java), content)
}

@Tunable
fun CircularRevealGridLayout(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit
) {
    Node(modifier.viewClass(CircularRevealGridLayout::class.java), content)
}

@Tunable
fun CircularRevealCoordinatorLayout(
    modifier: Modifier = Modifier,
    content: @Tunable CoordinatorLayoutScope.() -> Unit
) {
    Node(
        modifier = modifier.viewClass(CircularRevealCoordinatorLayout::class.java),
        content = { CoordinatorLayoutScopeInstance.content() }
    )
}

@Tunable
fun CircularRevealCardView(
    modifier: Modifier = Modifier,
    content: @Tunable CardScope.() -> Unit
) {
    Node(
        modifier = modifier.viewClass(CircularRevealCardView::class.java),
        content = { CardScopeInstance.content() }
    )
}