package com.huanli233.hibari.foundation

import android.widget.FrameLayout
import android.widget.LinearLayout
import com.huanli233.hibari.foundation.attributes.LinearOrientation
import com.huanli233.hibari.foundation.attributes.orientation
import com.huanli233.hibari.foundation.layout.LayoutScopeMarker
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenLayoutAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Row(
    modifier: Modifier = Modifier,
    content: @Tunable RowScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(LinearLayout::class.java)
            .orientation(LinearOrientation.HORIZONTAL),
        content = {
            RowScopeInstance.content()
        }
    )
}

internal object RowScopeInstance : RowScope {
    override fun Modifier.weight(weight: Float) = this.thenLayoutAttribute<LinearLayout.LayoutParams, Float>(uniqueKey, weight) { it, _ ->
        this.weight = it
    }

    override fun Modifier.gravity(gravity: Int) = this.thenLayoutAttribute<LinearLayout.LayoutParams, Int>(uniqueKey, gravity) { gravity, _ ->
        this.gravity = gravity
    }
}

@LayoutScopeMarker
interface RowScope {
    fun Modifier.weight(weight: Float): Modifier
    fun Modifier.gravity(gravity: Int): Modifier
}


@Tunable
fun Column(
    modifier: Modifier = Modifier,
    content: @Tunable ColumnScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(LinearLayout::class.java)
            .orientation(LinearOrientation.VERTICAL),
        content = {
            ColumnScopeInstance.content()
        }
    )
}

internal object ColumnScopeInstance : ColumnScope {
    override fun Modifier.weight(weight: Float) = this.thenLayoutAttribute<LinearLayout.LayoutParams, Float>(uniqueKey, weight) { it, _ ->
        this.weight = it
    }

    override fun Modifier.gravity(gravity: Int) = this.thenLayoutAttribute<LinearLayout.LayoutParams, Int>(uniqueKey, gravity) { gravity, _ ->
        this.gravity = gravity
    }
}

@LayoutScopeMarker
interface ColumnScope {
    fun Modifier.weight(weight: Float): Modifier
    fun Modifier.gravity(gravity: Int): Modifier
}