package com.huanli233.hibari.sample

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.huanli233.hibari.runtime.HibariDiffCallback
import com.huanli233.hibari.runtime.MutableState
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.Tuner
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.snapshots.Snapshot
import com.huanli233.hibari.ui.AttributeKey
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.ViewAttribute
import com.huanli233.hibari.ui.attribute
import com.huanli233.hibari.ui.node.Node

data class Dog(
    val name: String
)

fun main() {
    val tunable = @Tunable {
        // Re-using the same ListUpdateCallback from your example
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

        val ATTR_TITLE = AttributeKey<String>("title")
        val ATTR_PADDING = AttributeKey<Int>("padding")
        val ATTR_COLOR = AttributeKey<Int>("color")
        val ATTR_ALPHA = AttributeKey<Float>("alpha")

        println("--- SCENARIO 1: Reusable modifier changed ---")
        val oldList1 = listOf(
            Node(
                modifier = Modifier
                    .attribute(AttributeKey("padding"), 3f)
            )
        )
        val newList1 = listOf(
            Node(
                modifier = Modifier
                    .attribute(AttributeKey("padding"), 7f)
            )
        )

        val diffResult1 = DiffUtil.calculateDiff(HibariDiffCallback(oldList1, newList1))
        diffResult1.dispatchUpdatesTo(updateCallback)
        println()

        println("--- SCENARIO 2: Non-reusable modifier changed ---")
        val oldList2 = listOf(
            currentTuner.emitNode(
                Node(Modifier.attribute(AttributeKey("dog"), Dog("Test1"), false))
            )
        )
        val newList2 = listOf(
            currentTuner.emitNode(
                Node(Modifier.attribute(AttributeKey("dog"), Dog("Test"), false))
            )
        )

        val diffResult2 = DiffUtil.calculateDiff(HibariDiffCallback(oldList2, newList2))
        diffResult2.dispatchUpdatesTo(updateCallback)
        println()

        println("--- SCENARIO 3: Modifier added/removed ---")
        val oldList3 = listOf(
            currentTuner.emitNode(
                Node(Modifier.attribute(AttributeKey("title"), "test"))
            )
        )
        val newList3 = listOf(
            currentTuner.emitNode(Node(Modifier.attribute(AttributeKey("padding"), 7f).attribute(AttributeKey("title"), "test")))
        )

        val diffResult3 = DiffUtil.calculateDiff(HibariDiffCallback(oldList3, newList3))
        diffResult3.dispatchUpdatesTo(updateCallback)
        println()

        // --- SCENARIO 4: Multiple Reusable Changes on a Single Node ---
        // A single node has two of its reusable attributes changed.
        // Expected: One `onChanged` call with a payload containing BOTH changed attributes.
        println("--- SCENARIO 4: Multiple Reusable Changes on a Single Node ---")
        val oldList4 = listOf(
            currentTuner.emitNode(
                Node(Modifier
                    .attribute(ATTR_TITLE, "Old Title")
                    .attribute(ATTR_COLOR, 0xFF0000)
                )
            )
        )
        val newList4 = listOf(
            currentTuner.emitNode(Node(Modifier
                .attribute(ATTR_TITLE, "New Title") // Changed
                .attribute(ATTR_COLOR, 0x00FF00) // Changed
            ))
        )
        val diffResult4 = DiffUtil.calculateDiff(HibariDiffCallback(oldList4, newList4))
        diffResult4.dispatchUpdatesTo(updateCallback)
        println()

        // --- SCENARIO 5: A Moved Node Also Gets Updated ---
        // Item B moves from position 2 to 1, and its alpha value also changes.
        // Expected: `DiffUtil` typically dispatches moves first, then changes.
        // We should see a `onMoved` and an `onChanged` with a payload.
        println("--- SCENARIO 5: A Moved Node Also Gets Updated ---")
        val oldList5 = listOf(
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "Item A"))),
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_ALPHA, 1.0f))),
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "Item C")))
        )
        val newList5 = listOf(
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "Item A"))),
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "Item C"))),
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_ALPHA, 0.5f))) // Alpha changed
        )
        val diffResult5 = DiffUtil.calculateDiff(HibariDiffCallback(oldList5, newList5))
        diffResult5.dispatchUpdatesTo(updateCallback)
        println()

        // --- SCENARIO 6: Complex Mix of Operations ---
        // - Header (10) gets a reusable attribute change.
        // - Item A (20) is removed.
        // - Item C (40) is moved up.
        // - New Item (60) is inserted.
        // - Footer (50) remains stable.
        println("--- SCENARIO 6: Complex Mix of Operations ---")
        val oldList6 = listOf(
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "Old Header"))),
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_PADDING, 8))),  // To be removed
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_PADDING, 8))), // To be moved
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "Footer")))
        )
        val newList6 = listOf(
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "New Header"))), // Changed
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_PADDING, 8))),         // Moved
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "Newbie"))),   // Inserted
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "Footer")))
        )
        val diffResult6 = DiffUtil.calculateDiff(HibariDiffCallback(oldList6, newList6))
        diffResult6.dispatchUpdatesTo(updateCallback)
        println()

        // --- SCENARIO 7: Full Rebind Due to Mixed Reusable and Non-Reusable Changes ---
        // A node has both a reusable AND a non-reusable attribute change.
        // Expected: The presence of the non-reusable change should force a full rebind (no payload).
        println("--- SCENARIO 7: Full Rebind Due to Mixed Changes ---")
        val oldList7 = listOf(
            currentTuner.emitNode(
                Node(Modifier
                    .attribute(ATTR_TITLE, "Old Title")
                    .attribute(AttributeKey("dog"), Dog("Buddy"), reuseSupported = false)
                )
            )
        )
        val newList7 = listOf(
            currentTuner.emitNode(
                Node(Modifier
                    .attribute(ATTR_TITLE, "New Title") // Reusable change
                    .attribute(AttributeKey("dog"), Dog("Lucy"), reuseSupported = false) // Non-reusable change
                )
            )
        )
        val diffResult7 = DiffUtil.calculateDiff(HibariDiffCallback(oldList7, newList7))
        diffResult7.dispatchUpdatesTo(updateCallback)
        println()

        // --- SCENARIO 8: Diffing with Empty Lists ---
        // Part A: Initial creation (old list is empty)
        // Part B: Clearing the list (new list is empty)
        println("--- SCENARIO 8: Diffing with Empty Lists ---")
        println("Part A: Initial creation")
        val oldList8A = emptyList<Node>()
        val newList8A = listOf(
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "A"))),
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "B")))
        )
        val diffResult8A = DiffUtil.calculateDiff(HibariDiffCallback(oldList8A, newList8A))
        diffResult8A.dispatchUpdatesTo(updateCallback)

        println("\nPart B: Clearing the list")
        val oldList8B = listOf(
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "A"))),
            currentTuner.emitNode(Node(Modifier.attribute(ATTR_TITLE, "B")))
        )
        val newList8B = emptyList<Node>()
        val diffResult8B = DiffUtil.calculateDiff(HibariDiffCallback(oldList8B, newList8B))
        diffResult8B.dispatchUpdatesTo(updateCallback)
        println()
    }
}