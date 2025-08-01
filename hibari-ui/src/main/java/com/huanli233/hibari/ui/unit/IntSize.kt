@file:Suppress("NOTHING_TO_INLINE")

package com.huanli233.hibari.ui.unit

import com.huanli233.hibari.ui.util.fastRoundToInt
import com.huanli233.hibari.ui.util.packInts
import com.huanli233.hibari.ui.util.unpackInt1
import com.huanli233.hibari.ui.util.unpackInt2

/**
 * Constructs an [IntSize] from width and height [Int] values.
 */
fun IntSize(width: Int, height: Int): IntSize = IntSize(packInts(width, height))

/**
 * A two-dimensional size class used for measuring in [Int] pixels.
 */
@kotlin.jvm.JvmInline
value class IntSize internal constructor(@PublishedApi internal val packedValue: Long) {
    /**
     * The horizontal aspect of the size in [Int] pixels.
     */
    val width: Int
        get() = unpackInt1(packedValue)

    /**
     * The vertical aspect of the size in [Int] pixels.
     */
    val height: Int
        get() = unpackInt2(packedValue)

    inline operator fun component1(): Int = width

    inline operator fun component2(): Int = height

    /**
     * Returns an IntSize scaled by multiplying [width] and [height] by [other]
     */
    operator fun times(other: Int): IntSize = IntSize(
        packInts(
            unpackInt1(packedValue) * other,
            unpackInt2(packedValue) * other
        )
    )

    /**
     * Returns an IntSize scaled by dividing [width] and [height] by [other]
     */
    operator fun div(other: Int): IntSize = IntSize(
        packInts(
            unpackInt1(packedValue) / other,
            unpackInt2(packedValue) / other
        )
    )

    override fun toString(): String = "$width x $height"

    companion object {
        /**
         * IntSize with a zero (0) width and height.
         */
        val Zero = IntSize(0L)
    }
}

/**
 * Returns an [IntSize] with [size]'s [IntSize.width] and [IntSize.height]
 * multiplied by [this].
 */
inline operator fun Int.times(size: IntSize) = size * this

/**
 * Convert a [IntSize] to a [IntRect].
 */
fun IntSize.toIntRect(): IntRect {
    return IntRect(IntOffset.Zero, this)
}

/**
 * Returns the [IntOffset] of the center of the rect from the point of [0, 0]
 * with this [IntSize].
 */
val IntSize.center: IntOffset
    get() = IntOffset(
        // Divide X by 2 by moving it to the low bits, then place it back in the high bits
        (packedValue shr 33 shl 32)
                or
                // Move Y to the high bits so we can preserve the sign when dividing by 2, then
                // move Y back to the low bits and mask out the top 32 bits for X
                ((packedValue shl 32 shr 33) and 0xffffffffL)
    )

// temporary while PxSize is transitioned to Size
fun IntSize.toSize() = Size(width.toFloat(), height.toFloat())

/**
 * Convert a [Size] to an [IntSize]. This rounds the width and height values down to the nearest
 * integer.
 */
fun Size.toIntSize(): IntSize = IntSize(packInts(this.width.toInt(), this.height.toInt()))

/**
 * Convert a [Size] to an [IntSize]. This rounds [Size.width] and [Size.height] to the nearest
 * integer.
 */
fun Size.roundToIntSize(): IntSize = IntSize(
    packInts(
        this.width.fastRoundToInt(),
        this.height.fastRoundToInt()
    )
)
