package com.huanli233.hibari.material

import com.google.android.material.slider.Slider
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun Slider(
    value: Float = 0f,
    onValueChange: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    valueFrom: Float = 0f,
    valueTo: Float = 100f,
    stepSize: Float = 0f,
) {
    Node(
        modifier = modifier
            .viewClass(Slider::class.java)
            .thenViewAttribute<Slider, Float>(valueFrom) { from ->
                valueFrom = from
            }
            .thenViewAttribute<Slider, Float>(valueTo) { to ->
                valueTo = to
            }
            .thenViewAttribute<Slider, Float>(stepSize) { step ->
                stepSize = step
            }
            .thenViewAttribute<Slider, Float>(value) { sliderValue ->
                this.value = sliderValue
            }
            .thenViewAttribute<Slider, ((Float) -> Unit)?>(onValueChange) { onChange ->
                addOnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        onChange?.invoke(value)
                    }
                }
            }
    )
}