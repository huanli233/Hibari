package com.huanli233.hibari.material

import com.google.android.material.appbar.MaterialToolbar
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.attributes.text
import com.huanli233.hibari.ui.attributes.textColor
import com.huanli233.hibari.ui.attributes.textSize
import com.huanli233.hibari.ui.text.TextAlign
import com.huanli233.hibari.ui.unit.TextUnit
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun Toolbar(
    title: CharSequence? = null,
    modifier: Modifier = Modifier,
    titleTextColor: Int? = null,
    titleTextSize: TextUnit = TextUnit.Unspecified,
    titleTextAlign: TextAlign = TextAlign.Start,
    menuRes: Int? = null,
    onMenuItemClick: ((Int) -> Boolean)? = null,
) {
    Node(
        modifier = modifier
            .viewClass(MaterialToolbar::class.java)
            .thenViewAttribute<MaterialToolbar, CharSequence?>(title) { titleText ->
                title = titleText
            }
            .thenViewAttribute<MaterialToolbar, Int?>(menuRes) { menuResource ->
                if (menuResource != null) {
                    inflateMenu(menuResource)
                }
            }
            .thenViewAttribute<MaterialToolbar, ((Int) -> Boolean)?>(onMenuItemClick) { listener ->
                setOnMenuItemClickListener { menuItem ->
                    listener?.invoke(menuItem.itemId) ?: false
                }
            }
            .apply {
                if (titleTextColor != null) {
                    textColor(titleTextColor)
                }
            }
            .textSize(titleTextSize)
            .textAlign(titleTextAlign)
    )
}