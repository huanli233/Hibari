package com.huanli233.hibari.sample

import androidx.recyclerview.widget.ListUpdateCallback

data class Dog(
    val name: String
)

fun main() {
    // Re-using the same ListUpdateCallback from your example
    val updateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
        }
        override fun onRemoved(position: Int, count: Int) {
        }
        override fun onMoved(fromPosition: Int, toPosition: Int) {
            println("🔄 MOVED: from $fromPosition to $toPosition")
        }
        override fun onChanged(position: Int, count: Int, payload: Any?) {
        }
    }
}