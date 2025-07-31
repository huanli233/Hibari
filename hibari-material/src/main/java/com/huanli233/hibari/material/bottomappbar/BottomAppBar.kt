package com.huanli233.hibari.material.bottomappbar

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomappbar.BottomAppBar
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.node.Node
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun BottomAppBar(
    modifier: Modifier = Modifier,

    // FAB Integration
    @BottomAppBar.FabAlignmentMode fabAlignmentMode: Int? = null,
    @BottomAppBar.FabAnimationMode fabAnimationMode: Int? = null,
    @BottomAppBar.FabAnchorMode fabAnchorMode: Int? = null,

    // FAB Cradle Styling
    fabCradleMargin: Float? = null,
    fabCradleRoundedCornerRadius: Float? = null,
    cradleVerticalOffset: Float? = null,

    // Behavior & Appearance
    hideOnScroll: Boolean? = null,
    @MenuRes menuRes: Int? = null,
    @BottomAppBar.MenuAlignmentMode menuAlignmentMode: Int? = null,
    navigationIcon: Any? = null,
    backgroundTint: ColorStateList? = null,
    elevation: Float? = null,

    // Listeners
    onNavigationClick: (() -> Unit)? = null,
    onMenuItemClick: ((item: MenuItem) -> Boolean)? = null,
    onScrollStateChanged: ((view: View, state: Int) -> Unit)? = null,
) {
    Node(
        modifier = modifier
            .viewClass(BottomAppBar::class.java)
            // FAB
            .thenViewAttributeIfNotNull<BottomAppBar, Int>(
                uniqueKey,
                fabAlignmentMode
            ) { this.fabAlignmentMode = it }
            .thenViewAttributeIfNotNull<BottomAppBar, Int>(
                uniqueKey,
                fabAnimationMode
            ) { this.fabAnimationMode = it }
            .thenViewAttributeIfNotNull<BottomAppBar, Int>(
                uniqueKey,
                fabAnchorMode
            ) { this.fabAnchorMode = it }

            // Cradle
            .thenViewAttributeIfNotNull<BottomAppBar, Float>(
                uniqueKey,
                fabCradleMargin
            ) { this.fabCradleMargin = it }
            .thenViewAttributeIfNotNull<BottomAppBar, Float>(
                uniqueKey,
                fabCradleRoundedCornerRadius
            ) { this.fabCradleRoundedCornerRadius = it }
            .thenViewAttributeIfNotNull<BottomAppBar, Float>(
                uniqueKey,
                cradleVerticalOffset
            ) { this.cradleVerticalOffset = it }

            // Behavior & Appearance
            .thenViewAttributeIfNotNull<BottomAppBar, Boolean>(
                uniqueKey,
                hideOnScroll
            ) { this.hideOnScroll = it }
            .thenViewAttributeIfNotNull<BottomAppBar, Int>(uniqueKey, menuRes) { replaceMenu(it) }
            .thenViewAttributeIfNotNull<BottomAppBar, Int>(
                uniqueKey,
                menuAlignmentMode
            ) { this.menuAlignmentMode = it }
            .thenViewAttributeIfNotNull<BottomAppBar, Any>(uniqueKey, navigationIcon) {
                when (it) {
                    is Int -> setNavigationIcon(it)
                    is Drawable -> setNavigationIcon(it)
                }
            }
            .thenViewAttributeIfNotNull<BottomAppBar, ColorStateList>(
                uniqueKey,
                backgroundTint
            ) { this.backgroundTint = it }
            .thenViewAttributeIfNotNull<BottomAppBar, Float>(
                uniqueKey,
                elevation
            ) { this.elevation = it }

            // Listeners
            .thenViewAttributeIfNotNull<BottomAppBar, () -> Unit>(
                uniqueKey,
                onNavigationClick
            ) { setNavigationOnClickListener { it() } }
            .thenViewAttributeIfNotNull<BottomAppBar, (MenuItem) -> Boolean>(
                uniqueKey,
                onMenuItemClick
            ) { setOnMenuItemClickListener(it) }
            .thenViewAttributeIfNotNull<BottomAppBar, (View, Int) -> Unit>(
                uniqueKey,
                onScrollStateChanged
            ) { listener ->
                val behavior = getBehavior()
                behavior.addOnScrollStateChangedListener { bottomView, newState ->
                    listener(
                        bottomView,
                        newState
                    )
                }
            }
    )
}