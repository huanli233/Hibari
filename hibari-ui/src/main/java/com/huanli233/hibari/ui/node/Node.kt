package com.huanli233.hibari.ui.node

import com.huanli233.hibari.ui.Modifier

data class Node(
    var key: Int,
    val modifier: Modifier = Modifier,
    val children: List<Node> = emptyList(),
)