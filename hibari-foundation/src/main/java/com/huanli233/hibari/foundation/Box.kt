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
fun Box(
    modifier: Modifier = Modifier,
    content: @Tunable BoxScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(FrameLayout::class.java),
        content = {
            BoxScopeInstance.content()
        }
    )
}

object BoxScopeInstance : BoxScope {
    override fun Modifier.gravity(gravity: Int) = this.thenLayoutAttribute<FrameLayout.LayoutParams, Int>(uniqueKey, gravity) { gravity, _ ->
        this.gravity = gravity
    }
}

@LayoutScopeMarker
interface BoxScope {
    fun Modifier.gravity(gravity: Int): Modifier
}
