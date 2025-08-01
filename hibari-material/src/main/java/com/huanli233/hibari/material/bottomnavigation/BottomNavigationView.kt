package com.huanli233.hibari.material.bottomnavigation

import android.view.Menu
import androidx.annotation.DrawableRes
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

data class BottomNavigationItem(
    val id: Int,
    val title: CharSequence,
    @DrawableRes val icon: Int
)

@Tunable
fun BottomNavigation(
    modifier: Modifier = Modifier,
    items: List<BottomNavigationItem>,
    selectedItemId: Int,
    onItemSelected: (itemId: Int) -> Boolean,
    itemHorizontalTranslationEnabled: Boolean? = null,
) {
    Node(
        modifier = modifier
            .viewClass(BottomNavigationView::class.java)
            .thenViewAttribute<BottomNavigationView, List<BottomNavigationItem>>(uniqueKey, items) {
                // Dynamically populate the menu from the list of items
                menu.clear()
                it.forEachIndexed { index, item ->
                    menu.add(Menu.NONE, item.id, index, item.title).setIcon(item.icon)
                }
            }
            .thenViewAttribute<BottomNavigationView, Int>(uniqueKey, selectedItemId) {
                // Set the currently selected item
                if (this.selectedItemId != it) {
                    this.selectedItemId = it
                }
            }
            .thenViewAttribute<BottomNavigationView, (Int) -> Boolean>(uniqueKey, onItemSelected) { listener ->
                // Use the current, non-deprecated listener
                setOnItemSelectedListener { menuItem -> listener(menuItem.itemId) }
            }
            .thenViewAttributeIfNotNull<BottomNavigationView, Boolean>(uniqueKey, itemHorizontalTranslationEnabled) {
                this.isItemHorizontalTranslationEnabled = it
            }
    )
}