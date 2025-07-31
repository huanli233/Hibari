package com.huanli233.hibari.runtime

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.huanli233.hibari.ui.Attribute
import com.huanli233.hibari.ui.AttributeKey
import com.huanli233.hibari.ui.AttrsAttribute
import com.huanli233.hibari.ui.ViewClassAttribute
import com.huanli233.hibari.ui.flattenToList
import com.huanli233.hibari.ui.layoutAttributes
import com.huanli233.hibari.ui.node.Node
import com.huanli233.hibari.ui.viewAttributes

class HibariDiffCallback(
    private val oldList: List<Node>,
    private val newList: List<Node>
) : DiffUtil.Callback() {

    data class ModifierChangePayload(val changedAttributes: List<Attribute<*>>, val isChildrenChanged: Boolean)

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].key == newList[newItemPosition].key
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldNode = oldList[oldItemPosition]
        val newNode = newList[newItemPosition]

        val oldMods = oldNode.modifier.flattenToList()
        val newMods = newNode.modifier.flattenToList()

        val oldViewAttrs = oldMods.viewAttributes().associateBy { it.key }
        val newViewAttrs = newMods.viewAttributes().associateBy { it.key }
        val oldLayoutAttrs = oldMods.layoutAttributes().associateBy { it.key }
        val newLayoutAttrs = newMods.layoutAttributes().associateBy { it.key }

        val oldViewClass = (oldMods.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute)?.viewClass
            ?: hibariRuntimeError("The view class cannot be null.")
        val oldAttrXml = (oldMods.firstOrNull { it is AttrsAttribute } as? AttrsAttribute)?.attrs ?: -1
        val newViewClass = (newMods.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute)?.viewClass
            ?: hibariRuntimeError("The view class cannot be null.")
        val newAttrXml = (newMods.firstOrNull { it is AttrsAttribute } as? AttrsAttribute)?.attrs ?: -1

        if (oldViewClass != newViewClass || oldAttrXml != newAttrXml) {
            return false
        }

        for (key in newViewAttrs.keys) {
            val oldMod = oldViewAttrs[key]
            val newMod = newViewAttrs[key] ?: return false

            if (oldMod == null) {
                if (!newMod.reuseSupported) return false
            } else if (oldMod != newMod) {
                if (!newMod.reuseSupported) return false
            }
        }

        for (key in newLayoutAttrs.keys) {
            val oldMod = oldLayoutAttrs[key]
            val newMod = newLayoutAttrs[key] ?: return false

            if (oldMod == null) {
                if (!newMod.reuseSupported) return false
            } else if (oldMod != newMod) {
                if (!newMod.reuseSupported) return false
            }
        }

        if (oldNode.children != newNode.children) {
            return false
        }

        return true
    }


    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldNode = oldList[oldItemPosition]
        val newNode = newList[newItemPosition]

        val oldMods = oldNode.modifier.flattenToList()
        val newMods = newNode.modifier.flattenToList()

        val oldViewAttrs = oldMods.viewAttributes().associateBy { it.key }
        val newViewAttrs = newMods.viewAttributes().associateBy { it.key }
        val oldLayoutAttrs = oldMods.layoutAttributes().associateBy { it.key }
        val newLayoutAttrs = newMods.layoutAttributes().associateBy { it.key }

        val oldViewClass = (oldMods.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute)?.viewClass
            ?: hibariRuntimeError("The view class cannot be null.")
        val oldAttrXml = (oldMods.firstOrNull { it is AttrsAttribute } as? AttrsAttribute)?.attrs ?: -1
        val newViewClass = (newMods.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute)?.viewClass
            ?: hibariRuntimeError("The view class cannot be null.")
        val newAttrXml = (newMods.firstOrNull { it is AttrsAttribute } as? AttrsAttribute)?.attrs ?: -1

        if (oldViewClass != newViewClass || oldAttrXml != newAttrXml) {
            return null
        }

        val changedReusableMods = mutableListOf<Attribute<*>>()

        for (key in newViewAttrs.keys) {
            val oldMod = oldViewAttrs[key]
            val newMod = newViewAttrs[key] ?: return null

            if (oldMod == null) {
                if (!newMod.reuseSupported) return null
                changedReusableMods.add(newMod)
            } else if (oldMod != newMod) {
                if (!newMod.reuseSupported) return null
                changedReusableMods.add(newMod)
            }
        }

        for (key in newLayoutAttrs.keys) {
            val oldMod = oldLayoutAttrs[key]
            val newMod = newLayoutAttrs[key] ?: return null

            if (oldMod == null) {
                if (!newMod.reuseSupported) return null
                changedReusableMods.add(newMod)
            } else if (oldMod != newMod) {
                if (!newMod.reuseSupported) return null
                changedReusableMods.add(newMod)
            }
        }

        // 不再对 key 集合是否一致做硬性要求，而是合并检查差异是否可重用
        return if (changedReusableMods.isEmpty() && oldNode.children == newNode.children) {
            null
        } else {
            ModifierChangePayload(changedReusableMods, oldNode.children != newNode.children)
        }
    }

}