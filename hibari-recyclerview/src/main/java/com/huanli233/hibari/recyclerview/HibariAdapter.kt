package com.huanli233.hibari.recyclerview

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.Tunation

data class LazyListItem(
    val key: Any?,
    val contentType: Any?,
    val content: @Tunable () -> Unit
) {

    override fun hashCode(): Int {
        var result = key?.hashCode() ?: 0
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + content.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LazyListItem

        if (key != other.key) return false
        if (contentType != other.contentType) return false

        return true
    }
}

class HibariAdapter(
    private val parentTunation: Tunation
) : ListAdapter<LazyListItem, HibariViewHolder>(ItemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HibariViewHolder {
        val container = FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return HibariViewHolder(container, parentTunation)
    }

    override fun onBindViewHolder(holder: HibariViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item.content)
    }

    override fun onViewRecycled(holder: HibariViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).contentType?.hashCode() ?: 0
    }
    
    companion object {
        val ItemCallback = object : DiffUtil.ItemCallback<LazyListItem>() {
            override fun areItemsTheSame(oldItem: LazyListItem, newItem: LazyListItem): Boolean {
                return oldItem.key == newItem.key
            }
            override fun areContentsTheSame(oldItem: LazyListItem, newItem: LazyListItem): Boolean {
                val result = oldItem == newItem
                return result
            }
        }
    }
}