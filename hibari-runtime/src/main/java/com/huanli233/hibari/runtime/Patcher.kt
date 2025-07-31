package com.huanli233.hibari.runtime

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.huanli233.hibari.runtime.Renderer.Companion.hibariNodeKey
import com.huanli233.hibari.ui.ViewClassAttribute
import com.huanli233.hibari.ui.flattenToList
import com.huanli233.hibari.ui.node.Node
import com.huanli233.hibari.ui.util.ViewHierarchyPrinter

private const val TAG = "HibariDiff"

class Patcher(val renderer: Renderer) {

    private val viewToNodeMap = mutableMapOf<View, Node>()

    private fun ViewGroup.findViewByKey(key: Any?): View? {
        return this.children.find { it.getTag(hibariNodeKey) == key }
    }

    fun patch(parentView: ViewGroup, oldChildren: List<Node>, newChildren: List<Node>) {
        val parentId = parentView.javaClass.simpleName + "@" + System.identityHashCode(parentView).toString(16)
        Log.i(TAG, ">>> Starting patch for parent: $parentId | oldSize=${oldChildren.size}, newSize=${newChildren.size}")

        if (oldChildren === newChildren) {
            Log.i(TAG, "<<< Patch skipped for $parentId, lists are identical.")
            return
        }

        val diffCallback = HibariDiffCallback(oldChildren, newChildren)
        val diffResult = DiffUtil.calculateDiff(diffCallback, false)

        val updateCallback = object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                Log.d(TAG, "onInserted(pos=$position, count=$count) on parent $parentId")
                for (i in 0 until count) {
                    val newNode = newChildren[position + i]
                    val newView = renderer.render(newNode, parentView)
                    viewToNodeMap[newView] = newNode
                    parentView.addView(newView, position + i)
                    Log.d(TAG, "  -> Inserted view ${newView.javaClass.simpleName} for node with key ${newNode.key}")
                    if (newView is ViewGroup && newNode.children.isNotEmpty()) {
                        patch(newView, emptyList(), newNode.children)
                    }
                }
            }

            override fun onRemoved(position: Int, count: Int) {
                Log.d(TAG, "onRemoved(pos=$position, count=$count) on parent $parentId")
                for (i in 0 until count) {
                    val oldNode = oldChildren[position + i]
                    val viewToRemove = parentView.findViewByKey(oldNode.key)
                    if (viewToRemove != null) {
                        parentView.removeView(viewToRemove)
                        Log.d(TAG, "  -> Removed view ${viewToRemove.javaClass.simpleName} with key ${oldNode.key}")
                    } else {
                        Log.e(TAG, "  -> FAILED to find view to remove for key ${oldNode.key}")
                    }
                }
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                Log.d(TAG, "onMoved(from=$fromPosition, to=$toPosition) on parent $parentId")
                val nodeToMove = oldChildren[fromPosition]
                val viewToMove = parentView.findViewByKey(nodeToMove.key)

                if (viewToMove != null) {
                    parentView.removeView(viewToMove)
                    parentView.addView(viewToMove, toPosition)
                    Log.d(TAG, "  -> Moved view ${viewToMove.javaClass.simpleName} with key ${nodeToMove.key}")
                } else {
                    Log.e(TAG, "  -> FAILED to find view to move for key ${nodeToMove.key}")
                }
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                Log.d(TAG, "onChanged(pos=$position, count=$count) on parent $parentId | payload: ${payload != null}")
                for (i in 0 until count) {
                    val currentPos = position + i
                    val newNode = newChildren[position + i]

                    val viewToUpdate = parentView.findViewByKey(newNode.key)

                    if (viewToUpdate == null) {
                        Log.e(TAG, "  -> FATAL: Cannot find view for key ${newNode.key} to apply changes.")
                        continue
                    }

                    val oldNode = oldChildren.find { it.key == newNode.key }

                    val nodeViewClass = (newNode.modifier.flattenToList().firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute)
                        ?.viewClass?.simpleName ?: "UnknownNode"
                    Log.i(TAG, "  -> Preparing to update view: ${viewToUpdate.javaClass.simpleName} [key=${newNode.key}] with data from node for: $nodeViewClass")
                    if (viewToUpdate.javaClass.simpleName != nodeViewClass && nodeViewClass != "UnknownNode") {
                        Log.e(TAG, "  -> UNEXPECTED MISMATCH! View's class differs from initial render.")
                    }

                    if (payload != null && payload is HibariDiffCallback.ModifierChangePayload) {
                        // --- 局部更新 ---
                        Log.d(TAG, "  -> Applying PARTIAL update to ${viewToUpdate.javaClass.simpleName} at pos $currentPos")
                        if (payload.changedAttributes.isNotEmpty()) {
                            renderer.applyAttributes(viewToUpdate, payload.changedAttributes)
                        }
                        if (payload.isChildrenChanged && viewToUpdate is ViewGroup && oldNode != null) {
                            patch(viewToUpdate, oldNode.children, newNode.children)
                        }
                    } else {
                        // --- 完全重新绑定 ---
                        Log.w(TAG, "  -> Applying FULL rebind to view at pos $currentPos")
                        viewToNodeMap.remove(viewToUpdate)
                        parentView.removeViewAt(currentPos)

                        val newView = renderer.render(newNode, parentView)
                        viewToNodeMap[newView] = newView.let { newNode }
                        parentView.addView(newView, currentPos)
                    }
                    viewToNodeMap[viewToUpdate] = newNode
                }
            }
        }

        diffResult.dispatchUpdatesTo(updateCallback)
        Log.i(TAG, "<<< Patch finished for parent: $parentId")
    }
}