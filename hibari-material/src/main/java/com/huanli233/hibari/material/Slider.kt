package com.huanli233.hibari.material

import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Slider(
    value: Float,
    onValueChange: (value: Float) -> Unit,
    modifier: Modifier = Modifier,
    valueFrom: Float? = null,
    valueTo: Float? = null,
    stepSize: Float? = null
) {
    Node(
        modifier = modifier
            .viewClass(Slider::class.java)
            .thenViewAttribute<Slider, Float>(uniqueKey, value) {
                if (this.value != it) this.value = it
            }
            .thenViewAttribute<Slider, (Float) -> Unit>(uniqueKey, onValueChange) { listener ->
                clearOnSliderTouchListeners()
                addOnChangeListener { _, value, _ -> listener(value) }
            }
            .thenViewAttributeIfNotNull<Slider, Float>(uniqueKey, valueFrom) { this.valueFrom = it }
            .thenViewAttributeIfNotNull<Slider, Float>(uniqueKey, valueTo) { this.valueTo = it }
            .thenViewAttributeIfNotNull<Slider, Float>(uniqueKey, stepSize) { this.stepSize = it }
    )
}

@Tunable
fun RangeSlider(
    values: List<Float>,
    onValueChange: (values: List<Float>) -> Unit,
    modifier: Modifier = Modifier,
    valueFrom: Float? = null,
    valueTo: Float? = null,
    stepSize: Float? = null
) {
    Node(
        modifier = modifier
            .viewClass(RangeSlider::class.java)
            .thenViewAttribute<RangeSlider, List<Float>>(uniqueKey, values) {
                if (this.values != it) this.values = it
            }
            .thenViewAttribute<RangeSlider, (List<Float>) -> Unit>(uniqueKey, onValueChange) { listener ->
                clearOnSliderTouchListeners()
                addOnChangeListener { _, _, _ -> listener(this.values) }
            }
            .thenViewAttributeIfNotNull<RangeSlider, Float>(uniqueKey, valueFrom) { this.valueFrom = it }
            .thenViewAttributeIfNotNull<RangeSlider, Float>(uniqueKey, valueTo) { this.valueTo = it }
            .thenViewAttributeIfNotNull<RangeSlider, Float>(uniqueKey, stepSize) { this.stepSize = it }
    )
}