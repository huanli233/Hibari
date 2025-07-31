package com.huanli233.hibari.material

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun BottomNavigation(
    modifier: Modifier = Modifier,
    menuRes: Int? = null,
    selectedItemId: Int = 0,
    onItemSelected: ((Int) -> Unit)? = null,
) {
    Node(
        modifier = modifier
            .viewClass(BottomNavigationView::class.java)
            .thenViewAttribute<BottomNavigationView, Int?>(menuRes) { menuResource ->
                if (menuResource != null) {
                    inflateMenu(menuResource)
                }
            }
            .thenViewAttribute<BottomNavigationView, Int>(selectedItemId) { itemId ->
                selectedItemId = itemId
            }
            .thenViewAttribute<BottomNavigationView, ((Int) -> Unit)?>(onItemSelected) { listener ->
                setOnItemSelectedListener { menuItem ->
                    listener?.invoke(menuItem.itemId)
                    true
                }
            }
    )
}