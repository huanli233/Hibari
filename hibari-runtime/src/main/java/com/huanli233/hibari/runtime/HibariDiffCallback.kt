package com.huanli233.hibari.runtime

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.huanli233.hibari.ui.Attribute
import com.huanli233.hibari.ui.AttrsAttribute
import com.huanli233.hibari.ui.ViewClassAttribute
import com.huanli233.hibari.ui.flattenToList
import com.huanli233.hibari.ui.layoutAttributes
import com.huanli233.hibari.ui.node.Node
import com.huanli233.hibari.ui.viewAttributes
import java.util.Objects

private const val TAG = "HibariDiff"

class HibariDiffCallback(
    private val oldList: List<Node>,
    private val newList: List<Node>
) : DiffUtil.Callback() {

    data class ModifierChangePayload(val changedAttributes: List<Attribute<*>>, val isChildrenChanged: Boolean)

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldKey = oldList[oldItemPosition].key
        val newKey = newList[newItemPosition].key
        val result = Objects.equals(oldKey, newKey)
        return result
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldNode = oldList[oldItemPosition]
        val newNode = newList[newItemPosition]

        val childrenSame = oldNode.children == newNode.children
        if (!childrenSame) {
            return false
        }

        val modifierSame = oldNode.modifier == newNode.modifier
        if (modifierSame) {
            return true
        }

        // 深度比较
        val oldMods = oldNode.modifier.flattenToList()
        val newMods = newNode.modifier.flattenToList()

        if (oldMods.size != newMods.size) {
            return false
        }

        // 此处省略了详细的 map 比较日志，因为它们可能非常冗长
        // 最终结果是决定性的
        val result = (oldMods.viewAttributes().associateBy { it.key } == newMods.viewAttributes().associateBy { it.key }) &&
                (oldMods.layoutAttributes().associateBy { it.key } == newMods.layoutAttributes().associateBy { it.key }) &&
                (oldMods.firstOrNull { it is ViewClassAttribute } == newMods.firstOrNull { it is ViewClassAttribute }) &&
                (oldMods.firstOrNull { it is AttrsAttribute } == newMods.firstOrNull { it is AttrsAttribute })

        return result
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldNode = oldList[oldItemPosition]
        val newNode = newList[newItemPosition]

        val oldMods = oldNode.modifier.flattenToList()
        val newMods = newNode.modifier.flattenToList()

        val oldViewClassAttr = oldMods.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute
        val newViewClassAttr = newMods.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute
        if (oldViewClassAttr?.viewClass != newViewClassAttr?.viewClass) {
            return null
        }

        // ...其他基本检查...

        val oldViewAttrs = oldMods.viewAttributes().associateBy { it.key }
        val newViewAttrs = newMods.viewAttributes().associateBy { it.key }
        val oldLayoutAttrs = oldMods.layoutAttributes().associateBy { it.key }
        val newLayoutAttrs = newMods.layoutAttributes().associateBy { it.key }

        val changedReusableMods = mutableListOf<Attribute<*>>()
        val allOldAttrs = oldViewAttrs + oldLayoutAttrs
        val allNewAttrs = newViewAttrs + newLayoutAttrs
        val allKeys = allOldAttrs.keys + allNewAttrs.keys

        for (key in allKeys) {
            val oldAttr = allOldAttrs[key]
            val newAttr = allNewAttrs[key]

            if (oldAttr == newAttr) continue

            if (newAttr != null) {
                if (newAttr.reuseSupported) {
                    changedReusableMods.add(newAttr)
                } else {
                    return null
                }
            } else {
                return null
            }
        }

        val isChildrenChanged = oldNode.children != newNode.children

        return if (changedReusableMods.isNotEmpty() || isChildrenChanged) {
            val payload = ModifierChangePayload(changedReusableMods, isChildrenChanged)
            payload
        } else {
            null
        }
    }
}