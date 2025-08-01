package com.huanli233.hibari.material

import android.os.Build
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenLayoutAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun RelativeLayout(
    modifier: Modifier = Modifier,
    content: @Tunable RelativeLayoutScope.() -> Unit
) {
    Node(
        modifier = modifier.viewClass(RelativeLayout::class.java),
        content = { RelativeLayoutScopeInstance.content() }
    )
}

/**
 * Provides a scope for children of [RelativeLayout], enabling access to
 * modifiers for relative positioning.
 */
interface RelativeLayoutScope {
    private fun Modifier.addRule(verb: Int, subject: Int? = null): Modifier =
        this.thenLayoutAttribute<RelativeLayout.LayoutParams, Pair<Int, Int?>>(uniqueKey, verb to subject) { it, _ ->
            if (subject == null) addRule(it.first) else addRule(it.first, it.second!!)
        }

    fun Modifier.alignParentTop(align: Boolean = true): Modifier = addRule(RelativeLayout.ALIGN_PARENT_TOP, if (align) RelativeLayout.TRUE else 0)
    fun Modifier.alignParentBottom(align: Boolean = true): Modifier = addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, if (align) RelativeLayout.TRUE else 0)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun Modifier.alignParentStart(align: Boolean = true): Modifier = addRule(RelativeLayout.ALIGN_PARENT_START, if (align) RelativeLayout.TRUE else 0)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun Modifier.alignParentEnd(align: Boolean = true): Modifier = addRule(RelativeLayout.ALIGN_PARENT_END, if (align) RelativeLayout.TRUE else 0)

    fun Modifier.centerInParent(center: Boolean = true): Modifier = addRule(RelativeLayout.CENTER_IN_PARENT, if (center) RelativeLayout.TRUE else 0)
    fun Modifier.centerHorizontal(center: Boolean = true): Modifier = addRule(RelativeLayout.CENTER_HORIZONTAL, if (center) RelativeLayout.TRUE else 0)
    fun Modifier.centerVertical(center: Boolean = true): Modifier = addRule(RelativeLayout.CENTER_VERTICAL, if (center) RelativeLayout.TRUE else 0)

    fun Modifier.alignTop(@IdRes id: Int): Modifier = addRule(RelativeLayout.ALIGN_TOP, id)
    fun Modifier.alignBottom(@IdRes id: Int): Modifier = addRule(RelativeLayout.ALIGN_BOTTOM, id)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun Modifier.alignStart(@IdRes id: Int): Modifier = addRule(RelativeLayout.ALIGN_START, id)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun Modifier.alignEnd(@IdRes id: Int): Modifier = addRule(RelativeLayout.ALIGN_END, id)
    fun Modifier.alignBaseline(@IdRes id: Int): Modifier = addRule(RelativeLayout.ALIGN_BASELINE, id)

    fun Modifier.above(@IdRes id: Int): Modifier = addRule(RelativeLayout.ABOVE, id)
    fun Modifier.below(@IdRes id: Int): Modifier = addRule(RelativeLayout.BELOW, id)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun Modifier.toStartOf(@IdRes id: Int): Modifier = addRule(RelativeLayout.START_OF, id)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun Modifier.toEndOf(@IdRes id: Int): Modifier = addRule(RelativeLayout.END_OF, id)
}

internal object RelativeLayoutScopeInstance : RelativeLayoutScope