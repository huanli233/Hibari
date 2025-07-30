package com.huanli233.hibari.runtime

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

    data class ModifierChangePayload(val changedAttributes: List<Attribute<*>>)

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
        val newViewClass = (oldMods.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute)?.viewClass
            ?: hibariRuntimeError("The view class cannot be null.")
        val newAttrXml = (oldMods.firstOrNull { it is AttrsAttribute } as? AttrsAttribute)?.attrs ?: -1

        if (oldViewClass != newViewClass || oldAttrXml != newAttrXml) {
            return false
        }

        if (oldViewAttrs.keys != oldViewAttrs.keys) {
            return false
        }

        for (key in newViewAttrs.keys) {
            val oldMod = oldViewAttrs.getValue(key)
            val newMod = newViewAttrs.getValue(key)
            if (!newMod.reuseSupported && oldMod != newMod) {
                return false
            }
        }

        for (key in newLayoutAttrs.keys) {
            val oldMod = oldLayoutAttrs.getValue(key)
            val newMod = newLayoutAttrs.getValue(key)
            if (!newMod.reuseSupported && oldMod != newMod) {
                return false
            }
        }

        return oldMods == newMods
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
        val newViewClass = (oldMods.firstOrNull { it is ViewClassAttribute } as? ViewClassAttribute)?.viewClass
            ?: hibariRuntimeError("The view class cannot be null.")
        val newAttrXml = (oldMods.firstOrNull { it is AttrsAttribute } as? AttrsAttribute)?.attrs ?: -1

        if (oldViewClass != newViewClass || oldAttrXml != newAttrXml) {
            return null
        }

        if (oldViewAttrs.keys != oldViewAttrs.keys) {
            return null
        }

        val changedReusableMods = mutableListOf<Attribute<*>>()

        for (key in newViewAttrs.keys) {
            val oldMod = oldViewAttrs.getValue(key)
            val newMod = newViewAttrs.getValue(key)

            if (oldMod != newMod) {
                if (!newMod.reuseSupported) {
                    return null
                }
                changedReusableMods.add(newMod)
            }
        }

        for (key in newLayoutAttrs.keys) {
            val oldMod = oldLayoutAttrs.getValue(key)
            val newMod = newLayoutAttrs.getValue(key)

            if (oldMod != newMod) {
                if (!newMod.reuseSupported) {
                    return null
                }
                changedReusableMods.add(newMod)
            }
        }

        return if (changedReusableMods.isNotEmpty()) {
            ModifierChangePayload(changedReusableMods)
        } else {
            null
        }
    }
}