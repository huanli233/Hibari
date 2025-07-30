package com.huanli233.hibari.runtime

import android.view.ViewGroup
import com.huanli233.hibari.ui.node.Node

class Tunation(
    val hostView: ViewGroup,
    val content: @Tunable () -> Unit
) {
    var previousNodeList: List<Node> = emptyList()
    var memory: MutableMap<String, Any?> = mutableMapOf()
}