package com.huanli233.hibari.sample

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.HibariDiffCallback
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.Tuner
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.ui.AttributeKey
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.attribute

val updateCallback = object : ListUpdateCallback {
    override fun onInserted(position: Int, count: Int) {
        println("✅ INSERTED: at position $position, count $count")
    }
    override fun onRemoved(position: Int, count: Int) {
        println("❌ REMOVED: from position $position, count $count")
    }
    override fun onMoved(fromPosition: Int, toPosition: Int) {
        println("🔄 MOVED: from $fromPosition to $toPosition")
    }
    override fun onChanged(position: Int, count: Int, payload: Any?) {
        if (payload != null) {
            println("✨ UPDATED (reusable): at position $position with payload: $payload")
        } else {
            println("💥 UPDATED (full rebind): at position $position")
        }
    }
}

fun main() {
    val tuner1 = Tuner()
    val tuner2 = Tuner()
    val dogKey = AttributeKey<String>("dog")
    var flag by mutableStateOf(false)
    val tunable = @Tunable {
        Node(
            modifier = Modifier
                .attribute(dogKey, if (flag) "🐶" else "🐱"),
            children = listOf()
        )
    }
    DiffUtil.calculateDiff(HibariDiffCallback(tuner1.nodes, tuner2.nodes)).dispatchUpdatesTo(updateCallback)
}