package com.huanli233.hibari.ui.node

import android.content.Context
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.unit.Constraints

data class Node(
    val modifier: Modifier = Modifier,
    var children: List<Node> = emptyList(),
    var key: String? = null,
    val measurePolicy: MeasurePolicy? = null
)

fun interface MeasurePolicy {
    fun measure(measurables: List<Measurable>, constraints: Constraints): Placeable
}

interface Measurable {
    val parentData: Any?
    val context: Context
    fun measure(constraints: Constraints): Placeable
}

abstract class Placeable {
    var width: Int = 0
        protected set
    var height: Int = 0
        protected set

    abstract fun placeAt(x: Int, y: Int)
}