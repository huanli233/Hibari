package com.huanli233.hibari.foundation

import android.annotation.SuppressLint
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.node.MeasurePolicy
import com.huanli233.hibari.ui.node.Node

@SuppressLint("TunableNamingDetector")
@Tunable
fun Node(
    modifier: Modifier = Modifier,
    content: @Tunable () -> Unit = {}
) = currentTuner.emitNode(
    Node(modifier), content
)

@SuppressLint("TunableNamingDetector")
@Tunable
fun Node(
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy,
    content: @Tunable () -> Unit = {}
) = currentTuner.emitNode(
    Node(modifier, measurePolicy = measurePolicy), content
)