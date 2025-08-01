package com.huanli233.hibari.material.navigation

import android.graphics.drawable.Drawable
import android.view.Menu
import androidx.annotation.DrawableRes
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

data class NavigationItem(
    val id: Int,
    val title: CharSequence,
    val icon: Any
)

@Tunable
fun NavigationView(
    modifier: Modifier = Modifier,
    items: List<NavigationItem>,
    checkedItemId: Int?,
    onItemSelected: (itemId: Int) -> Boolean,
    header: @Tunable (() -> Unit)? = null
) {
    Node(
        modifier = modifier
            .viewClass(NavigationView::class.java)
            .thenViewAttribute<NavigationView, List<NavigationItem>>(uniqueKey, items) {
                menu.clear()
                it.forEachIndexed { index, item ->
                    menu.add(Menu.NONE, item.id, index, item.title).run {
                        when (item.icon) {
                            is Int -> setIcon(item.icon)
                            is Drawable -> icon = item.icon
                            else -> error("Unsupported icon type: ${item.icon::class.java}")
                        }
                    }
                }
            }
            .thenViewAttributeIfNotNull<NavigationView, Int>(uniqueKey, checkedItemId) {
                if (this.checkedItem?.itemId != it) {
                    this.setCheckedItem(it)
                }
            }
            .thenViewAttribute<NavigationView, (Int) -> Boolean>(uniqueKey, onItemSelected) { listener ->
                setNavigationItemSelectedListener { menuItem -> listener(menuItem.itemId) }
            },
        content = {
            header?.invoke()
        }
    )
}

@Tunable
fun NavigationRailView(
    modifier: Modifier = Modifier,
    items: List<NavigationItem>,
    selectedItemId: Int,
    onItemSelected: (itemId: Int) -> Boolean,
    header: @Tunable (() -> Unit)? = null,
) {
    Node(
        modifier = modifier
            .viewClass(NavigationRailView::class.java)
            .thenViewAttribute<NavigationRailView, List<NavigationItem>>(uniqueKey, items) {
                menu.clear()
                it.forEachIndexed { index, item ->
                    menu.add(Menu.NONE, item.id, index, item.title).run {
                        when (item.icon) {
                            is Int -> setIcon(item.icon)
                            is Drawable -> icon = item.icon
                            else -> error("Unsupported icon type: ${item.icon::class.java}")
                        }
                    }
                }
            }
            .thenViewAttribute<NavigationRailView, Int>(uniqueKey, selectedItemId) {
                if (this.selectedItemId != it) {
                    this.selectedItemId = it
                }
            }
            .thenViewAttribute<NavigationRailView, (Int) -> Boolean>(uniqueKey, onItemSelected) { listener ->
                setOnItemSelectedListener { menuItem -> listener(menuItem.itemId) }
            },
        content = {
            header?.invoke()
        }
    )
}