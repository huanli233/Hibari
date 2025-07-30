package com.huanli233.hibari.sample

import androidx.recyclerview.widget.ListUpdateCallback

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
}