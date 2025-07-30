package com.huanli233.hibari.foundation

import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.node.Node

@Tunable
fun Node(
    modifier: Modifier = Modifier,
    children: List<Node> = emptyList()
) = currentTuner.emitNode(
    Node(modifier, children)
)