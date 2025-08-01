package com.huanli233.hibari.material

import android.widget.GridLayout
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenLayoutAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun GridLayout(
    modifier: Modifier = Modifier,
    rowCount: Int? = null,
    columnCount: Int? = null,
    content: @Tunable GridLayoutScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(GridLayout::class.java)
            .thenViewAttributeIfNotNull<GridLayout, Int>(uniqueKey, rowCount) { this.rowCount = it }
            .thenViewAttributeIfNotNull<GridLayout, Int>(uniqueKey, columnCount) { this.columnCount = it },
        content = { GridLayoutScopeInstance.content() }
    )
}

interface GridLayoutScope {
    fun Modifier.layoutGravity(gravity: Int): Modifier =
        this.thenLayoutAttribute<GridLayout.LayoutParams, Int>(uniqueKey, gravity) { it, _ -> this.setGravity(it) }

    fun Modifier.layoutRow(row: Int, rowSpan: Int = 1): Modifier = this
        .thenLayoutAttribute<GridLayout.LayoutParams, Int>(uniqueKey, row) { it, _ -> rowSpec = GridLayout.spec(it, 1) }
        .thenLayoutAttribute<GridLayout.LayoutParams, Int>(uniqueKey, rowSpan) { it, _ -> rowSpec = GridLayout.spec(it) }

    fun Modifier.layoutColumn(col: Int, colSpan: Int = 1): Modifier = this
        .thenLayoutAttribute<GridLayout.LayoutParams, Int>(uniqueKey, col) { it, _ -> columnSpec = GridLayout.spec(it, 1) }
        .thenLayoutAttribute<GridLayout.LayoutParams, Int>(uniqueKey, colSpan) { it, _ -> columnSpec = GridLayout.spec(it) }
}

internal object GridLayoutScopeInstance : GridLayoutScope