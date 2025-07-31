package com.huanli233.hibari.material

import android.graphics.drawable.Drawable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Drawable? = null,
    contentDescription: String? = null
) {
    Node(
        modifier = modifier
            .viewClass(FloatingActionButton::class.java)
            .onClick(onClick)
    )
}