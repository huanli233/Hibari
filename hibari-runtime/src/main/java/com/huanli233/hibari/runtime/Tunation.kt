package com.huanli233.hibari.runtime

import android.view.ViewGroup
import com.huanli233.hibari.ui.node.Node

class Tunation(
    val hostView: ViewGroup,
    var content: @Tunable () -> Unit,
    var tuner: Tuner? = null
) {

    constructor(
        hostView: ViewGroup,
        content: @Tunable () -> Unit,
        tuner: Tuner? = null,
        parent: Tunation
    ) : this(hostView, content, tuner) {
        this.localValueStacks = parent.localValueStacks
        this.memory = parent.memory
    }

    var localValueStacks: TunationLocalsHashMap = tunationLocalHashMapOf()
    var lastTree: List<Node> = emptyList()
    var memory: MutableMap<String, Any?> = mutableMapOf()

    fun dispose() {
        SnapshotManager.clearDependencies(this)
    }
}