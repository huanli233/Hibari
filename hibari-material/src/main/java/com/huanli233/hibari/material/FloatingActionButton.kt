package com.huanli233.hibari.material

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun FloatingActionButton(
    modifier: Modifier = Modifier,
    icon: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    Node(
        modifier = modifier
            .viewClass(FloatingActionButton::class.java)
            .thenViewAttribute<FloatingActionButton, Int?>(icon) { iconRes ->
                if (iconRes != null) {
                    setImageResource(iconRes)
                }
            }
            .thenViewAttribute<FloatingActionButton, (() -> Unit)?>(onClick) { clickListener ->
                setOnClickListener { clickListener?.invoke() }
            }
    )
}