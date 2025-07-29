package com.huanli233.hibari.runtime

import androidx.recyclerview.widget.DiffUtil
import com.huanli233.hibari.ui.Attribute
import com.huanli233.hibari.ui.AttributeKey
import com.huanli233.hibari.ui.flattenToList
import com.huanli233.hibari.ui.node.Node

class HibariDiffCallback(
    private val oldList: List<Node>,
    private val newList: List<Node>
) : DiffUtil.Callback() {

    data class ModifierChangePayload(val changedAttributes: List<Attribute>)

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].key == newList[newItemPosition].key
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldNode = oldList[oldItemPosition]
        val newNode = newList[newItemPosition]

        val oldMods = oldNode.modifier.flattenToList().associateBy { it.key }
        val newMods = newNode.modifier.flattenToList().associateBy { it.key }

        if (oldMods[AttributeKey<String>("viewClass")] != newMods[AttributeKey<String>("viewClass")]) {
            return false
        }

        if (oldMods.keys != newMods.keys) {
            return false
        }

        for (key in newMods.keys) {
            val oldMod = oldMods.getValue(key)
            val newMod = newMods.getValue(key)
            if (!newMod.reuseSupported && oldMod != newMod) {
                return false
            }
        }

        return oldMods == newMods
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldNode = oldList[oldItemPosition]
        val newNode = newList[newItemPosition]

        val oldMods = oldNode.modifier.flattenToList().associateBy { it.key }
        val newMods = newNode.modifier.flattenToList().associateBy { it.key }

        if (oldMods[AttributeKey<String>("viewClass")] != newMods[AttributeKey<String>("viewClass")]) {
            return null
        }

        if (oldMods.keys != newMods.keys) return null

        val changedReusableMods = mutableListOf<Attribute>()

        for (key in newMods.keys) {
            val oldMod = oldMods.getValue(key)
            val newMod = newMods.getValue(key)

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