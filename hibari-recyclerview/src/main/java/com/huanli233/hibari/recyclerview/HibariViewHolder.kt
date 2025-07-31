package com.huanli233.hibari.recyclerview

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.huanli233.hibari.runtime.GlobalRetuner
import com.huanli233.hibari.runtime.SnapshotManager
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.Tunation
import kotlinx.coroutines.cancel

class HibariViewHolder(
    private val container: ViewGroup,
    private val parentTunation: Tunation
) : RecyclerView.ViewHolder(container) {

    private var itemTunation: Tunation? = null

    fun bind(content: @Tunable () -> Unit) {
        if (itemTunation == null) {
            itemTunation = Tunation(
                hostView = container,
                content = content,
                parent = parentTunation
            )
        } else {
            itemTunation!!.content = content
        }

        GlobalRetuner.retuner.tuneNow(itemTunation!!)
    }

    fun recycle() {
        itemTunation?.let {
            SnapshotManager.clearDependencies(it)
            it.tuner?.coroutineScope?.cancel()
        }
        itemTunation = null
        container.removeAllViews()
    }
}