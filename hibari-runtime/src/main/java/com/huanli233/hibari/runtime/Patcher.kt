package com.huanli233.hibari.runtime

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.huanli233.hibari.ui.flattenToList
import com.huanli233.hibari.ui.node.Node

class Patcher(val renderer: Renderer) {

    private val viewToNodeMap = mutableMapOf<View, Node>()

    fun patch(parentView: ViewGroup, oldChildren: List<Node>, newChildren: List<Node>) {
        val diffCallback = HibariDiffCallback(oldChildren, newChildren)
        val diffResult = DiffUtil.calculateDiff(diffCallback, false)

        val updateCallback = object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                println("$parentView ✅ INSERTED: at position $position, count $count")
                for (i in 0 until count) {
                    val newNode = newChildren[position + i]
                    val newView = renderer.render(newNode, parentView)
                    viewToNodeMap[newView] = newNode
                    parentView.addView(newView, position + i)
                    if (newView is ViewGroup) {
                        patch(newView, emptyList(), newNode.children)
                    }
                }
            }

            override fun onRemoved(position: Int, count: Int) {
                println("$parentView ❌ REMOVED: from position $position, count $count")
                for (i in 0 until count) {
                    val viewToRemove = parentView.getChildAt(position)
                    viewToNodeMap.remove(viewToRemove)
                    parentView.removeViewAt(position)
                }
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                println("$parentView 🔄 MOVED: from $fromPosition to $toPosition")
                val view = parentView.getChildAt(fromPosition)
                parentView.removeViewAt(fromPosition)
                parentView.addView(view, toPosition)
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                if (payload != null) {
                    println("$parentView ✨ UPDATED (reusable): at position $position with payload: $payload")
                } else {
                    println("$parentView 💥 UPDATED (full rebind): at position $position")
                }
                for (i in 0 until count) {
                    val currentPos = position + i
                    var viewToUpdate = parentView.getChildAt(currentPos)
                    val oldNode = oldChildren.getOrNull(currentPos)
                    val newNode = newChildren.getOrNull(currentPos)
                    if (payload != null && viewToUpdate != null && payload is HibariDiffCallback.ModifierChangePayload) {
                        renderer.applyAttributes(viewToUpdate, payload.changedAttributes)
                    } else {
                        if (viewToUpdate != null) {
                            parentView.removeViewAt(currentPos)
                        }
                        newNode?.let { viewToUpdate = renderer.render(it, parentView) }
                        parentView.addView(viewToUpdate, currentPos)
                        if (viewToUpdate is ViewGroup) {
                            newNode?.children?.let { patch(viewToUpdate, emptyList(), it) }
                        }
                    }
                    newNode?.let { viewToNodeMap[viewToUpdate] = it }

                    if (viewToUpdate is ViewGroup && oldNode != null && newNode != null) {
                        patch(viewToUpdate, oldNode.children, newNode.children)
                    }
                }
            }
        }

        diffResult.dispatchUpdatesTo(updateCallback)
    }

}