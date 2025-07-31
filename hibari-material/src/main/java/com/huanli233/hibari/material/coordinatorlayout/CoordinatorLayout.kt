package com.huanli233.hibari.material.coordinatorlayout

import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.IdRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenLayoutAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun CoordinatorLayout(
    modifier: Modifier = Modifier,
    statusBarBackground: Any? = null,
    content: @Tunable CoordinatorLayoutScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(CoordinatorLayout::class.java)
            .thenViewAttributeIfNotNull<CoordinatorLayout, Any>(uniqueKey, statusBarBackground) {
                when (it) {
                    is Int -> {
                        try {
                            setStatusBarBackgroundColor(it)
                        } catch (_: Exception) {
                            setStatusBarBackgroundResource(it)
                        }
                    }
                    is Drawable -> setStatusBarBackground(it)
                }
            },
        content = {
            CoordinatorLayoutScopeInstance.content()
        }
    )
}

interface CoordinatorLayoutScope {
    fun Modifier.layoutAnchor(@IdRes id: Int): Modifier =
        this.thenLayoutAttribute<CoordinatorLayout.LayoutParams, Int>(uniqueKey, id) { it, _ -> anchorId = it }

    fun Modifier.layoutAnchorGravity(gravity: Int): Modifier =
        this.thenLayoutAttribute<CoordinatorLayout.LayoutParams, Int>(uniqueKey, gravity) { it, _ -> anchorGravity = it }

    fun Modifier.layoutBehavior(behavior: CoordinatorLayout.Behavior<out View>): Modifier =
        this.thenLayoutAttribute<CoordinatorLayout.LayoutParams, CoordinatorLayout.Behavior<out View>>(uniqueKey, behavior) { it, _ -> setBehavior(it) }

    fun Modifier.gravity(gravity: Int): Modifier =
        this.thenLayoutAttribute<CoordinatorLayout.LayoutParams, Int>(uniqueKey, gravity) { it, _ -> this.gravity = it }
}

internal object CoordinatorLayoutScopeInstance : CoordinatorLayoutScope