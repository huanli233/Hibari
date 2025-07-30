package com.huanli233.hibari.ui.node

import com.huanli233.hibari.ui.Modifier

data class Node(
    val modifier: Modifier = Modifier,
    val children: List<Node>? = emptyList(),
    var key: String? = null,
)