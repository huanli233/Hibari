package com.huanli233.hibari.material

import android.graphics.drawable.Drawable
import com.google.android.material.button.MaterialButton
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.text
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Button(
    text: CharSequence,
    modifier: Modifier = Modifier,
    icon: Drawable? = null
) {
    Node(
        modifier = modifier
            .viewClass(MaterialButton::class.java)
            .text(text)
    )
}