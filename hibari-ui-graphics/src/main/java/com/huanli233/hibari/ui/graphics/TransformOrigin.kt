package com.huanli233.hibari.ui.graphics

import com.huanli233.hibari.ui.util.packFloats
import com.huanli233.hibari.ui.util.unpackFloat1
import com.huanli233.hibari.ui.util.unpackFloat2

fun TransformOrigin(pivotFractionX: Float, pivotFractionY: Float): TransformOrigin =
    TransformOrigin(packFloats(pivotFractionX, pivotFractionY))

/**
 * A two-dimensional position represented as a fraction of the Layer's width and height
 */
@JvmInline
value class TransformOrigin internal constructor(@PublishedApi internal val packedValue: Long) {

    /**
     * Return the position along the x-axis that should be used as the
     * origin for rotation and scale transformations. This is represented as a fraction
     * of the width of the content. A value of 0.5f represents the midpoint between the left
     * and right bounds of the content
     */
    val pivotFractionX: Float
        get() = unpackFloat1(packedValue)

    /**
     * Return the position along the y-axis that should be used as the
     * origin for rotation and scale transformations. This is represented as a fraction
     * of the height of the content. A value of 0.5f represents the midpoint between the top
     * and bottom bounds of the content
     */
    val pivotFractionY: Float
        get() = unpackFloat2(packedValue)

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun component1(): Float = pivotFractionX

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun component2(): Float = pivotFractionY

    /**
     * Returns a copy of this TransformOrigin instance optionally overriding the
     * pivotFractionX or pivotFractionY parameter
     */
    fun copy(
        pivotFractionX: Float = this.pivotFractionX,
        pivotFractionY: Float = this.pivotFractionY
    ) = TransformOrigin(pivotFractionX, pivotFractionY)

    companion object {

        /**
         * [TransformOrigin] constant to indicate that the center of the content should
         * be used for rotation and scale transformations
         */
        val Center = TransformOrigin(0.5f, 0.5f)
    }
}
