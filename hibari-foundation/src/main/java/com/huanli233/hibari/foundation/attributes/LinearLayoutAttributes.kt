package com.huanli233.hibari.foundation.attributes

import android.widget.LinearLayout
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.uniqueKey

enum class LinearOrientation {
    VERTICAL,
    HORIZONTAL
}

fun Modifier.orientation(orientation: LinearOrientation): Modifier {
    return this.thenViewAttribute<LinearLayout, LinearOrientation>(uniqueKey, orientation) {
        when (it) {
            LinearOrientation.VERTICAL -> setOrientation(LinearLayout.VERTICAL)
            LinearOrientation.HORIZONTAL -> setOrientation(LinearLayout.HORIZONTAL)
        }
    }
}