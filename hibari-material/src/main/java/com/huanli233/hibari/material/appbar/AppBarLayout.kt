package com.huanli233.hibari.material.appbar

import android.view.animation.Interpolator
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.layout.LayoutScopeMarker
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenLayoutAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun AppBarLayout(
    modifier: Modifier = Modifier,
    content: @Tunable AppBarLayoutScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(AppBarLayout::class.java),
        content = {
            AppBarLayoutScopeInstance.content()
        }
    )
}

@LayoutScopeMarker
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
    content: @Tunable MaterialToolbarScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(MaterialToolbar::class.java),
        content = {
            MaterialToolbarScopeInstance.content()
        }
    )
}

@LayoutScopeMarker
interface MaterialToolbarScope
internal object MaterialToolbarScopeInstance : MaterialToolbarScope

@Tunable
fun CollapsingToolbar(
    modifier: Modifier = Modifier,
    content: @Tunable CollapsingToolbarScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(CollapsingToolbarLayout::class.java),
        content = {
            CollapsingToolbarScopeInstance.content()
        }
    )
}

@LayoutScopeMarker
interface CollapsingToolbarScope {
    fun Modifier.collapseMode(collapseMode: Int): Modifier = this.thenLayoutAttribute<CollapsingToolbarLayout.LayoutParams, Int>(uniqueKey, collapseMode) { it, _ ->
        this.collapseMode = it
    }
    fun Modifier.parallaxMultiplier(multiplier: Float): Modifier = this.thenLayoutAttribute<CollapsingToolbarLayout.LayoutParams, Float>(uniqueKey, multiplier) { it, _ ->
        this.parallaxMultiplier = multiplier
    }
}
internal object CollapsingToolbarScopeInstance : CollapsingToolbarScope
