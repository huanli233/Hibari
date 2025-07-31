package com.huanli233.hibari.foundation

import android.widget.Space
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Spacer(modifier: Modifier = Modifier) {
    Node(
        modifier = modifier.viewClass(Space::class.java)
    )
}