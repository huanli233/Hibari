package com.huanli233.hibari.material.appbar

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.animation.Interpolator
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenLayoutAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass
import kotlin.math.min

@Tunable
fun AppBarLayout(
    modifier: Modifier = Modifier,
    orientation: Int? = null,
    expanded: Boolean? = null,
    animateExpansion: Boolean = true,
    liftOnScroll: Boolean? = null,
    lifted: Boolean? = null,
    @IdRes liftOnScrollTargetViewId: Int? = null,
    statusBarForeground: Any? = null,
    onOffsetChanged: ((appBarLayout: AppBarLayout, verticalOffset: Int) -> Unit)? = null,
    onLiftStateChanged: ((lift: Float, elevation: Int) -> Unit)? = null,
    content: @Tunable AppBarLayoutScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(AppBarLayout::class.java)
            .thenViewAttributeIfNotNull<AppBarLayout, Int>(uniqueKey, orientation) { this.orientation = it }
            .thenViewAttributeIfNotNull<AppBarLayout, Boolean>(uniqueKey, expanded) { setExpanded(it, animateExpansion) }
            .thenViewAttributeIfNotNull<AppBarLayout, Boolean>(uniqueKey, liftOnScroll) { this.isLiftOnScroll = it }
            .thenViewAttributeIfNotNull<AppBarLayout, Boolean>(uniqueKey, lifted) { this.isLifted = it }
            .thenViewAttributeIfNotNull<AppBarLayout, Int>(uniqueKey, liftOnScrollTargetViewId) { this.liftOnScrollTargetViewId = it }
            .thenViewAttributeIfNotNull<AppBarLayout, Any>(uniqueKey, statusBarForeground) {
                when (it) {
                    is Int -> {
                        try {
                            setStatusBarForegroundColor(it)
                        } catch (_: Exception) {
                            setStatusBarForegroundResource(it)
                        }
                    }
                    is Drawable -> setStatusBarForeground(it)
                }
            }
            .thenViewAttributeIfNotNull<AppBarLayout, (AppBarLayout, Int) -> Unit>(uniqueKey, onOffsetChanged) { listener ->
                addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener(listener))
            }
            .thenViewAttributeIfNotNull<AppBarLayout, (Float, Int) -> Unit>(uniqueKey, onLiftStateChanged) { listener ->
                addLiftOnScrollListener { lift, elevation -> listener(lift, elevation) }
            },
        content = {
            AppBarLayoutScopeInstance.content()
        }
    )
}

interface AppBarLayoutScope {
    fun Modifier.scrollFlags(flags: Int): Modifier = this.thenLayoutAttribute< AppBarLayout.LayoutParams, Int>(uniqueKey, flags) { it, _ ->
        this.scrollFlags = it
    }
    fun Modifier.scrollEffect(effect: Int): Modifier = this.thenLayoutAttribute< AppBarLayout.LayoutParams, Int>(uniqueKey, effect) { it, _ ->
        this.setScrollEffect(it)
    }
    fun Modifier.scrollInterpolator(interpolator: Interpolator): Modifier = this.thenLayoutAttribute< AppBarLayout.LayoutParams, Interpolator>(uniqueKey, interpolator) { it, _ ->
        this.scrollInterpolator = it
    }
}

internal object AppBarLayoutScopeInstance : AppBarLayoutScope

@Tunable
fun MaterialToolbar(
    modifier: Modifier = Modifier,

    // Core Content
    title: CharSequence? = null,
    subtitle: CharSequence? = null,
    titleCentered: Boolean? = null,
    subtitleCentered: Boolean? = null,

    // Navigation Icon
    navigationIcon: Any? = null,
    @ColorInt navigationIconTint: Int? = null,

    // Logo
    logo: Any? = null,
    logoAdjustViewBounds: Boolean? = null,
    logoScaleType: ImageView.ScaleType? = null,

    // Menu
    @MenuRes menuRes: Int? = null,

    // Listeners
    onNavigationClick: (() -> Unit)? = null,
    onMenuItemClick: ((item: MenuItem) -> Boolean)? = null,

    content: @Tunable MaterialToolbarScope.() -> Unit = {}
) {
    Node(
        modifier = modifier
            .viewClass(MaterialToolbar::class.java)
            // Core
            .thenViewAttributeIfNotNull<MaterialToolbar, CharSequence>(uniqueKey, title) { this.title = it }
            .thenViewAttributeIfNotNull<MaterialToolbar, CharSequence>(uniqueKey, subtitle) { this.subtitle = it }
            .thenViewAttributeIfNotNull<MaterialToolbar, Boolean>(uniqueKey, titleCentered) { this.isTitleCentered = it }
            .thenViewAttributeIfNotNull<MaterialToolbar, Boolean>(uniqueKey, subtitleCentered) { this.isSubtitleCentered = it }

            // Navigation
            .thenViewAttributeIfNotNull<MaterialToolbar, Any>(uniqueKey, navigationIcon) {
                when (it) {
                    is Int -> setNavigationIcon(it)
                    is Drawable -> setNavigationIcon(it)
                }
            }
            .thenViewAttributeIfNotNull<MaterialToolbar, Int>(uniqueKey, navigationIconTint) { setNavigationIconTint(it) }

            // Logo
            .thenViewAttributeIfNotNull<MaterialToolbar, Any>(uniqueKey, logo) {
                when (it) {
                    is Int -> setLogo(it)
                    is Drawable -> setLogo(it)
                }
            }
            .thenViewAttributeIfNotNull<MaterialToolbar, Boolean>(uniqueKey, logoAdjustViewBounds) { this.isLogoAdjustViewBounds = it }
            .thenViewAttributeIfNotNull<MaterialToolbar, ImageView.ScaleType>(uniqueKey, logoScaleType) { this.setLogoScaleType(it) }

            // Menu
            .thenViewAttributeIfNotNull<MaterialToolbar, Int>(uniqueKey, menuRes) {
                // Clear previous menu items before inflating a new one to handle recomposition
                menu.clear()
                inflateMenu(it)
            }

            // Listeners
            .thenViewAttributeIfNotNull<MaterialToolbar, () -> Unit>(uniqueKey, onNavigationClick) { setNavigationOnClickListener { it() } }
            .thenViewAttributeIfNotNull<MaterialToolbar, (MenuItem) -> Boolean>(uniqueKey, onMenuItemClick) { setOnMenuItemClickListener(it) }
        ,
        content = {
            MaterialToolbarScopeInstance.content()
        }
    )
}

interface MaterialToolbarScope
internal object MaterialToolbarScopeInstance : MaterialToolbarScope

@SuppressLint("RestrictedApi")
@Tunable
fun CollapsingToolbar(
    modifier: Modifier = Modifier,

    // Title
    title: CharSequence? = null,
    titleEnabled: Boolean? = null,
    @CollapsingToolbarLayout.TitleCollapseMode titleCollapseMode: Int? = null,
    maxLines: Int? = null,

    // Collapsed Title Styling
    collapsedTitleGravity: Int? = null,
    collapsedTitleTextColor: Any? = null,
    collapsedTitleTypeface: Typeface? = null,
    @StyleRes collapsedTitleTextAppearance: Int? = null,

    // Expanded Title Styling
    expandedTitleGravity: Int? = null,
    expandedTitleTextColor: Any? = null,
    expandedTitleTypeface: Typeface? = null,
    @StyleRes expandedTitleTextAppearance: Int? = null,
    @Px expandedTitleMarginStart: Int? = null,
    @Px expandedTitleMarginTop: Int? = null,
    @Px expandedTitleMarginEnd: Int? = null,
    @Px expandedTitleMarginBottom: Int? = null,

    // Scrims
    contentScrim: Any? = null,
    statusBarScrim: Any? = null,
    scrimAnimationDuration: Long? = null,
    scrimVisibleHeightTrigger: Int? = null,
    scrimsShown: Boolean? = null,
    animateScrims: Boolean = true,

    content: @Tunable CollapsingToolbarScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(CollapsingToolbarLayout::class.java)
            // Title
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, CharSequence>(uniqueKey, title) { this.title = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Boolean>(uniqueKey, titleEnabled) { this.isTitleEnabled = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, titleCollapseMode) { this.titleCollapseMode = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, maxLines) { this.maxLines = it }

            // Collapsed Title
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, collapsedTitleGravity) { this.collapsedTitleGravity = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Any>(uniqueKey, collapsedTitleTextColor) {
                when(it) {
                    is ColorStateList -> setCollapsedTitleTextColor(it)
                    is Int -> setCollapsedTitleTextColor(it)
                }
            }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Typeface>(uniqueKey, collapsedTitleTypeface) { this.setCollapsedTitleTypeface(it) }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, collapsedTitleTextAppearance) { setCollapsedTitleTextAppearance(it) }

            // Expanded Title
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, expandedTitleGravity) { this.expandedTitleGravity = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Any>(uniqueKey, expandedTitleTextColor) {
                when(it) {
                    is ColorStateList -> setExpandedTitleTextColor(it)
                    is Int -> setExpandedTitleTextColor(ColorStateList.valueOf(it))
                }
            }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Typeface>(uniqueKey, expandedTitleTypeface) { this.setExpandedTitleTypeface(it)  }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, expandedTitleTextAppearance) { setExpandedTitleTextAppearance(it) }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, expandedTitleMarginStart) { this.expandedTitleMarginStart = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, expandedTitleMarginTop) { this.expandedTitleMarginTop = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, expandedTitleMarginEnd) { this.expandedTitleMarginEnd = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, expandedTitleMarginBottom) { this.expandedTitleMarginBottom = it }

            // Scrims
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Any>(uniqueKey, contentScrim) {
                when (it) {
                    is Int -> {
                        try { setContentScrimColor(it) }
                        catch (_: Exception) { setContentScrimResource(it) }
                    }
                    is Drawable -> setContentScrim(it)
                }
            }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Any>(uniqueKey, statusBarScrim) {
                when (it) {
                    is Int -> {
                        try { setStatusBarScrimColor(it) }
                        catch (_: Exception) { setStatusBarScrimResource(it) }
                    }
                    is Drawable -> setStatusBarScrim(it)
                }
            }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Long>(uniqueKey, scrimAnimationDuration) { this.scrimAnimationDuration = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Int>(uniqueKey, scrimVisibleHeightTrigger) { this.scrimVisibleHeightTrigger = it }
            .thenViewAttributeIfNotNull<CollapsingToolbarLayout, Boolean>(uniqueKey, scrimsShown) { setScrimsShown(it, animateScrims) }
        ,
        content = {
            CollapsingToolbarScopeInstance.content()
        }
    )
}


interface CollapsingToolbarScope {
    fun Modifier.collapseMode(collapseMode: Int): Modifier = this.thenLayoutAttribute<CollapsingToolbarLayout.LayoutParams, Int>(uniqueKey, collapseMode) { it, _ ->
        this.collapseMode = it
    }
    fun Modifier.parallaxMultiplier(multiplier: Float): Modifier = this.thenLayoutAttribute<CollapsingToolbarLayout.LayoutParams, Float>(uniqueKey, multiplier) { it, _ ->
        this.parallaxMultiplier = multiplier
    }
}
internal object CollapsingToolbarScopeInstance : CollapsingToolbarScope
