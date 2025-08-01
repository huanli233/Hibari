package com.huanli233.hibari.material.tabs

import android.graphics.drawable.Drawable
import com.google.android.material.tabs.TabLayout
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

data class TabItem(
    val text: CharSequence,
    val icon: Any? = null
)

@Tunable
fun TabLayout(
    modifier: Modifier = Modifier,
    items: List<TabItem>,
    selectedTabIndex: Int,
    onTabSelected: (index: Int) -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(TabLayout::class.java)
            .thenViewAttribute<TabLayout, List<TabItem>>(uniqueKey, items) {
                if (this.tabCount != it.size) {
                    this.removeAllTabs()
                    it.forEach { item ->
                        this.addTab(this.newTab().setText(item.text).run {
                            when (item.icon) {
                                is Int -> setIcon(item.icon)
                                is Drawable -> setIcon(item.icon)
                                else -> error("Unsupported icon type: ${item.icon!!::class.java}")
                            }
                        })
                    }
                } else {
                    it.forEachIndexed { index, item ->
                        this.getTabAt(index)?.let { tab ->
                            tab.text = item.text
                            when (item.icon) {
                                is Int -> tab.setIcon(item.icon)
                                is Drawable -> tab.setIcon(item.icon)
                                else -> error("Unsupported icon type: ${item.icon!!::class.java}")
                            }
                        }
                    }
                }
            }
            .thenViewAttribute<TabLayout, Int>(uniqueKey, selectedTabIndex) {
                if (this.selectedTabPosition != it) {
                    this.getTabAt(it)?.select()
                }
            }
            .thenViewAttribute<TabLayout, (Int) -> Unit>(uniqueKey, onTabSelected) { listener ->
                clearOnTabSelectedListeners() // Prevent duplicate listeners
                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        tab?.let { listener(it.position) }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
            }
    )
}