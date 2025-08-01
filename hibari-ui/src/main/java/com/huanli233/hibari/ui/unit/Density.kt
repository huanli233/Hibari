package com.huanli233.hibari.ui.unit

import com.huanli233.hibari.ui.unit.fontscaling.FontScaling
import com.huanli233.hibari.ui.util.fastRoundToInt

/**
 * A density of the screen. Used for convert [Dp] to pixels.
 *
 * @param density The logical density of the display. This is a scaling factor for the [Dp] unit.
 * @param fontScale Current user preference for the scaling factor for fonts.
 */
fun Density(density: Float, fontScale: Float = 1f): Density =
    DensityImpl(density, fontScale)

private data class DensityImpl(
    override val density: Float,
    override val fontScale: Float
) : Density

/**
 * A density of the screen. Used for the conversions between pixels, [Dp], [Int] and [TextUnit].
 */
interface Density : FontScaling {

    /**
     * The logical density of the display. This is a scaling factor for the [Dp] unit.
     */
    val density: Float

    /**
     * Convert [Dp] to pixels. Pixels are used to paint to Canvas.
     */
    fun Dp.toPx(): Float = value * density

    /**
     * Convert [Dp] to [Int] by rounding
     */
    fun Dp.roundToPx(): Int {
        val px = toPx()
        return if (px.isInfinite()) Constraints.Infinity else px.fastRoundToInt()
    }

    /**
     * Convert Sp to pixels. Pixels are used to paint to Canvas.
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    fun TextUnit.toPx(): Float {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return toDp().toPx()
    }

    /**
     * Convert Sp to [Int] by rounding
     */
    fun TextUnit.roundToPx(): Int = toPx().fastRoundToInt()

    /**
     * Convert an [Int] pixel value to [Dp].
     */
    fun Int.toDp(): Dp = (this / density).dp

    /**
     * Convert an [Int] pixel value to Sp.
     */
    fun Int.toSp(): TextUnit = toDp().toSp()

    /** Convert a [Float] pixel value to a Dp */
    fun Float.toDp(): Dp = (this / density).dp

    /** Convert a [Float] pixel value to a Sp */
    fun Float.toSp(): TextUnit = toDp().toSp()

    /**
     * Convert a [DpRect] to a [Rect].
     */
    fun DpRect.toRect(): Rect {
        return Rect(
            left.toPx(),
            top.toPx(),
            right.toPx(),
            bottom.toPx()
        )
    }

    /**
     * Convert a [DpSize] to a [Size].
     */
    fun DpSize.toSize(): Size = if (isSpecified) {
        Size(width.toPx(), height.toPx())
    } else {
        Size.Unspecified
    }

    /**
     * Convert a [Size] to a [DpSize].
     */
    fun Size.toDpSize(): DpSize = if (isSpecified) {
        DpSize(width.toDp(), height.toDp())
    } else {
        DpSize.Unspecified
    }
}
