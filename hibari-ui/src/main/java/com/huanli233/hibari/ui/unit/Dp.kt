@file:Suppress("NOTHING_TO_INLINE")

package com.huanli233.hibari.ui.unit

import android.content.Context
import android.view.View
import com.huanli233.hibari.ui.util.packFloats
import com.huanli233.hibari.ui.util.unpackFloat1
import com.huanli233.hibari.ui.util.unpackFloat2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@JvmInline
value class Dp(val value: Float) : Comparable<Dp> {
    /**
     * Add two [Dp]s together.
     */
    inline operator fun plus(other: Dp) = Dp(this.value + other.value)

    /**
     * Subtract a Dp from another one.
     */
    inline operator fun minus(other: Dp) = Dp(this.value - other.value)

    /**
     * This is the same as multiplying the Dp by -1.0.
     */
    inline operator fun unaryMinus() = Dp(-value)

    /**
     * Divide a Dp by a scalar.
     */
    inline operator fun div(other: Float): Dp = Dp(value / other)

    inline operator fun div(other: Int): Dp = Dp(value / other)

    /**
     * Divide by another Dp to get a scalar.
     */
    inline operator fun div(other: Dp): Float = value / other.value

    /**
     * Multiply a Dp by a scalar.
     */
    inline operator fun times(other: Float): Dp = Dp(value * other)

    inline operator fun times(other: Int): Dp = Dp(value * other)

    /**
     * Support comparing Dimensions with comparison operators.
     */
    override /* TODO: inline */ operator fun compareTo(other: Dp) = value.compareTo(other.value)

    override fun toString() = if (isUnspecified) "Dp.Unspecified" else "$value.dp"

    companion object {
        /**
         * A dimension used to represent a hairline drawing element. Hairline elements take up no
         * space, but will draw a single pixel, independent of the device's resolution and density.
         */
        val Hairline = Dp(0f)

        /**
         * Infinite dp dimension.
         */
        val Infinity = Dp(Float.POSITIVE_INFINITY)

        /**
         * Constant that means unspecified Dp
         */
        val Unspecified = Dp(Float.NaN)
    }
}

/**
 * `false` when this is [Dp.Unspecified].
 */
inline val Dp.isSpecified: Boolean
    get() = !value.isNaN()

/**
 * `true` when this is [Dp.Unspecified].
 */
inline val Dp.isUnspecified: Boolean
    get() = value.isNaN()

/**
 * If this [Dp] [isSpecified] then this is returned, otherwise [block] is executed
 * and its result is returned.
 */
inline fun Dp.takeOrElse(block: () -> Dp): Dp =
    if (isSpecified) this else block()

/**
 * Create a [Dp] using an [Int]:
 *     val left = 10
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.dp
 */
inline val Int.dp: Dp get() = Dp(this.toFloat())

/**
 * Create a [Dp] using a [Double]:
 *     val left = 10.0
 *     val x = left.dp
 *     // -- or --
 *     val y = 10.0.dp
 */
inline val Double.dp: Dp get() = Dp(this.toFloat())

/**
 * Create a [Dp] using a [Float]:
 *     val left = 10f
 *     val x = left.dp
 *     // -- or --
 *     val y = 10f.dp
 */
inline val Float.dp: Dp get() = Dp(this)

inline operator fun Float.times(other: Dp) =
    Dp(this * other.value)

inline operator fun Double.times(other: Dp) =
    Dp(this.toFloat() * other.value)

inline operator fun Int.times(other: Dp) =
    Dp(this * other.value)

inline fun min(a: Dp, b: Dp): Dp = Dp(min(a.value, b.value))

inline fun max(a: Dp, b: Dp): Dp = Dp(max(a.value, b.value))

/**
 * Ensures that this value lies in the specified range [minimumValue]..[maximumValue].
 *
 * @return this value if it's in the range, or [minimumValue] if this value is less than
 * [minimumValue], or [maximumValue] if this value is greater than [maximumValue].
 */
inline fun Dp.coerceIn(minimumValue: Dp, maximumValue: Dp): Dp =
    Dp(value.coerceIn(minimumValue.value, maximumValue.value))

/**
 * Ensures that this value is not less than the specified [minimumValue].
 * @return this value if it's greater than or equal to the [minimumValue] or the
 * [minimumValue] otherwise.
 */
inline fun Dp.coerceAtLeast(minimumValue: Dp): Dp =
    Dp(value.coerceAtLeast(minimumValue.value))

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 *
 * @return this value if it's less than or equal to the [maximumValue] or the
 * [maximumValue] otherwise.
 */
inline fun Dp.coerceAtMost(maximumValue: Dp): Dp =
    Dp(value.coerceAtMost(maximumValue.value))

/**
 *
 * Return `true` when it is finite or `false` when it is [Dp.Infinity]
 */
inline val Dp.isFinite: Boolean get() = value != Float.POSITIVE_INFINITY

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// Structures using Dp
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

/**
 * Constructs a [DpOffset] from [x] and [y] position [Dp] values.
 */
fun DpOffset(x: Dp, y: Dp): DpOffset = DpOffset(packFloats(x.value, y.value))

/**
 * A two-dimensional offset using [Dp] for units
 */
@JvmInline
value class DpOffset internal constructor(@PublishedApi internal val packedValue: Long) {
    /**
     * The horizontal aspect of the offset in [Dp]
     */
    val x: Dp get() = unpackFloat1(packedValue).dp

    /**
     * The vertical aspect of the offset in [Dp]
     */
    val y: Dp get() = unpackFloat2(packedValue).dp

    /**
     * Returns a copy of this [DpOffset] instance optionally overriding the
     * x or y parameter
     */
    fun copy(x: Dp = this.x, y: Dp = this.y): DpOffset = DpOffset(packFloats(x.value, y.value))

    /**
     * Subtract a [DpOffset] from another one.
     */
    operator fun minus(other: DpOffset) = DpOffset(
        packFloats(
            (x - other.x).value,
            (y - other.y).value
        )
    )

    /**
     * Add a [DpOffset] to another one.
     */
    operator fun plus(other: DpOffset) = DpOffset(
        packFloats(
            (x + other.x).value,
            (y + other.y).value
        )
    )

    override fun toString(): String =
        if (isSpecified) {
            "($x, $y)"
        } else {
            "DpOffset.Unspecified"
        }

    companion object {
        /**
         * A [DpOffset] with 0 DP [x] and 0 DP [y] values.
         */
        val Zero = DpOffset(0x0L)

        /**
         * Represents an offset whose [x] and [y] are unspecified. This is usually a replacement for
         * `null` when a primitive value is desired.
         * Access to [x] or [y] on an unspecified offset is not allowed.
         */
        val Unspecified = DpOffset(0x7fc00000_7fc00000L)
    }
}

/**
 * `false` when this is [DpOffset.Unspecified].
 */
inline val DpOffset.isSpecified: Boolean
    get() = packedValue != 0x7fc00000_7fc00000L // Keep UnspecifiedPackedFloats internal

/**
 * `true` when this is [DpOffset.Unspecified].
 */
inline val DpOffset.isUnspecified: Boolean
    get() = packedValue == 0x7fc00000_7fc00000L // Keep UnspecifiedPackedFloats internal

/**
 * If this [DpOffset]&nbsp;[isSpecified] then this is returned, otherwise [block] is executed
 * and its result is returned.
 */
inline fun DpOffset.takeOrElse(block: () -> DpOffset): DpOffset =
    if (isSpecified) this else block()


/**
 * Constructs a [DpSize] from [width] and [height] [Dp] values.
 */
fun DpSize(width: Dp, height: Dp): DpSize = DpSize(packFloats(width.value, height.value))

/**
 * A two-dimensional Size using [Dp] for units
 */
@JvmInline
value class DpSize internal constructor(@PublishedApi internal val packedValue: Long) {
    /**
     * The horizontal aspect of the Size in [Dp]
     */
    val width: Dp get() = unpackFloat1(packedValue).dp

    /**
     * The vertical aspect of the Size in [Dp]
     */
    val height: Dp get() = unpackFloat2(packedValue).dp

    /**
     * Returns a copy of this [DpSize] instance optionally overriding the
     * width or height parameter
     */
    fun copy(width: Dp = this.width, height: Dp = this.height): DpSize = DpSize(
        packFloats(width.value, height.value)
    )

    /**
     * Subtract a [DpSize] from another one.
     */
    operator fun minus(other: DpSize) =
        DpSize(
            packFloats(
                (width - other.width).value,
                (height - other.height).value
            )
        )

    /**
     * Add a [DpSize] to another one.
     */
    operator fun plus(other: DpSize) =
        DpSize(
            packFloats(
                (width + other.width).value,
                (height + other.height).value
            )
        )

    inline operator fun component1(): Dp = width

    inline operator fun component2(): Dp = height

    operator fun times(other: Int): DpSize = DpSize(
        packFloats(
            (width * other).value,
            (height * other).value
        )
    )

    operator fun times(other: Float): DpSize = DpSize(
        packFloats(
            (width * other).value,
            (height * other).value
        )
    )

    operator fun div(other: Int): DpSize = DpSize(
        packFloats(
            (width / other).value,
            (height / other).value
        )
    )

    operator fun div(other: Float): DpSize = DpSize(
        packFloats(
            (width / other).value,
            (height / other).value
        )
    )

    override fun toString(): String =
        if (isSpecified) {
            "$width x $height"
        } else {
            "DpSize.Unspecified"
        }

    companion object {
        /**
         * A [DpSize] with 0 DP [width] and 0 DP [height] values.
         */
        val Zero = DpSize(0x0L)

        /**
         * A size whose [width] and [height] are unspecified. This is usually a replacement for
         * `null` when a primitive value is desired.
         * Access to [width] or [height] on an unspecified size is not allowed.
         */
        val Unspecified = DpSize(0x7fc00000_7fc00000L)
    }
}

/**
 * `false` when this is [DpSize.Unspecified].
 */
inline val DpSize.isSpecified: Boolean
    get() = packedValue != 0x7fc00000_7fc00000L // Keep UnspecifiedPackedFloats internal

/**
 * `true` when this is [DpSize.Unspecified].
 */
inline val DpSize.isUnspecified: Boolean
    get() = packedValue == 0x7fc00000_7fc00000L // Keep UnspecifiedPackedFloats internal

/**
 * If this [DpSize]&nbsp;[isSpecified] then this is returned, otherwise [block] is executed
 * and its result is returned.
 */
inline fun DpSize.takeOrElse(block: () -> DpSize): DpSize =
    if (isSpecified) this else block()

/**
 * Returns the [DpOffset] of the center of the rect from the point of [0, 0]
 * with this [DpSize].
 */
val DpSize.center: DpOffset
    get() = DpOffset(
        packFloats(
            (width / 2f).value,
            (height / 2f).value
        )
    )

inline operator fun Int.times(size: DpSize) = size * this

inline operator fun Float.times(size: DpSize) = size * this

/**
 * A four dimensional bounds using [Dp] for units
 */
data class DpRect(
    val left: Dp,
    val top: Dp,
    val right: Dp,
    val bottom: Dp
) {
    /**
     * Constructs a [DpRect] from the top-left [origin] and the width and height in [size].
     */
    constructor(origin: DpOffset, size: DpSize) :
            this(origin.x, origin.y, origin.x + size.width, origin.y + size.height)

    companion object
}

/**
 * A width of this Bounds in [Dp].
 */
inline val DpRect.width: Dp get() = right - left

/**
 * A height of this Bounds in [Dp].
 */
inline val DpRect.height: Dp get() = bottom - top

/**
 * Returns the size of the [DpRect].
 */
inline val DpRect.size: DpSize get() = DpSize(width, height)


fun Dp.toPx(context: Context): Int {
    if (this.isUnspecified) {
        return 0
    }
    val density = context.resources.displayMetrics.density
    return (this.value * density).roundToInt()
}

fun Dp.toPx(view: View): Int = this.toPx(view.context)