package com.huanli233.hibari.material

import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun CircularProgressBar(
    progress: Int = 0,
    modifier: Modifier = Modifier,
    indeterminate: Boolean = false,
) {
    Node(
        modifier = modifier
            .viewClass(CircularProgressIndicator::class.java)
            .thenViewAttribute<CircularProgressIndicator, Int>(progress) { progressValue ->
                setProgress(progressValue, true)
            }
            .thenViewAttribute<CircularProgressIndicator, Boolean>(indeterminate) { isIndeterminate ->
                isIndeterminate = isIndeterminate
            }
    )
}

@Tunable
fun LinearProgressBar(
    progress: Int = 0,
    modifier: Modifier = Modifier,
    indeterminate: Boolean = false,
) {
    Node(
        modifier = modifier
            .viewClass(LinearProgressIndicator::class.java)
            .thenViewAttribute<LinearProgressIndicator, Int>(progress) { progressValue ->
                setProgress(progressValue, true)
            }
            .thenViewAttribute<LinearProgressIndicator, Boolean>(indeterminate) { isIndeterminate ->
                isIndeterminate = isIndeterminate
            }
    )
}