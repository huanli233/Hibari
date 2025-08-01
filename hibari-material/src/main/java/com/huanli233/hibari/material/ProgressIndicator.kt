package com.huanli233.hibari.material

import androidx.annotation.ColorInt
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Int? = null,
    indeterminate: Boolean? = null,
    @ColorInt indicatorColor: Int? = null,
) {
    Node(
        modifier = modifier
            .viewClass(CircularProgressIndicator::class.java)
            .thenViewAttributeIfNotNull<CircularProgressIndicator, Int>(uniqueKey, progress) {
                setProgressCompat(it, true)
            }
            .thenViewAttributeIfNotNull<CircularProgressIndicator, Boolean>(uniqueKey, indeterminate) { this.isIndeterminate = it }
            .thenViewAttributeIfNotNull<CircularProgressIndicator, Int>(uniqueKey, indicatorColor) { this.setIndicatorColor(it) }
    )
}

@Tunable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Int? = null,
    indeterminate: Boolean? = null,
    @ColorInt indicatorColor: Int? = null,
) {
    Node(
        modifier = modifier
            .viewClass(LinearProgressIndicator::class.java)
            .thenViewAttributeIfNotNull<LinearProgressIndicator, Int>(uniqueKey, progress) { setProgressCompat(it, true) }
            .thenViewAttributeIfNotNull<LinearProgressIndicator, Boolean>(uniqueKey, indeterminate) { this.isIndeterminate = it }
            .thenViewAttributeIfNotNull<LinearProgressIndicator, Int>(uniqueKey, indicatorColor) { this.setIndicatorColor(it) }
    )
}