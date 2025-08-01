package com.huanli233.hibari.material.floatingactionbutton

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun FloatingActionButton(
    modifier: Modifier = Modifier,
    icon: Any? = null,
    @FloatingActionButton.Size size: Int? = null,
) {
    Node(
        modifier = modifier
            .viewClass(FloatingActionButton::class.java)
            .thenViewAttributeIfNotNull<FloatingActionButton, Any>(uniqueKey, icon) {
                when (it) {
                    is Int -> setImageResource(it)
                    is Drawable -> setImageDrawable(it)
                }
            }
            .thenViewAttributeIfNotNull<FloatingActionButton, Int>(uniqueKey, size) { this.size = it }
    )
}

@Tunable
fun ExtendedFloatingActionButton(
    modifier: Modifier = Modifier,
    icon: Any? = null,
    text: String? = null,
) {
    Node(
        modifier = modifier
            .viewClass(FloatingActionButton::class.java)
            .thenViewAttributeIfNotNull<ExtendedFloatingActionButton, Any>(uniqueKey, icon) {
                when (it) {
                    is Int -> setIconResource(it)
                    is Drawable -> this.icon = it
                }
            }
            .thenViewAttributeIfNotNull<ExtendedFloatingActionButton, String>(uniqueKey, text) { this.text = it }
    )
}