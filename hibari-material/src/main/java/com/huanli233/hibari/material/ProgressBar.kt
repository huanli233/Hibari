package com.huanli233.hibari.material

import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun LinearProgressIndicator(
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    indeterminate: Boolean = false
) {
    Node(
        modifier = modifier
            .viewClass(LinearProgressIndicator::class.java)
    )
}

@Tunable
fun CircularProgressIndicator(
    progress: Float = 0f,
    modifier: Modifier = Modifier,
    indeterminate: Boolean = false
) {
    Node(
        modifier = modifier
            .viewClass(CircularProgressIndicator::class.java)
    )
}