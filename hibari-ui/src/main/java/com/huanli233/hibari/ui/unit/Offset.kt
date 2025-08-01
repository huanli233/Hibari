package com.huanli233.hibari.ui.unit

import com.huanli233.hibari.ui.util.DualFirstNaN
import com.huanli233.hibari.ui.util.DualFloatInfinityBase
import com.huanli233.hibari.ui.util.DualFloatSignBit
import com.huanli233.hibari.ui.util.DualUnsignedFloatMask
import com.huanli233.hibari.ui.util.Uint64High32
import com.huanli233.hibari.ui.util.Uint64Low32
import com.huanli233.hibari.ui.util.UnspecifiedPackedFloats
import com.huanli233.hibari.ui.util.lerp
import com.huanli233.hibari.ui.util.packFloats
import com.huanli233.hibari.ui.util.toStringAsFixed
import com.huanli233.hibari.ui.util.unpackFloat1
import com.huanli233.hibari.ui.util.unpackFloat2
import kotlin.isFinite
import kotlin.math.sqrt
import kotlin.times

/**
 * Constructs an Offset from the given relative x and y offsets
 */
fun Offset(x: Float, y: Float) = Offset(packFloats(x, y))

/**
 * An immutable 2D floating-point offset.
 *
 * Generally speaking, Offsets can be interpreted in two ways:
 *
 * 1. As representing a point in Cartesian space a specified distance from a
 *    separately-maintained origin. For example, the top-left position of
 *    children in the [RenderBox] protocol is typically represented as an
 *    [Offset] from the top left of the parent box.
 *
 * 2. As a vector that can be applied to coordinates. For example, when
 *    painting a [RenderObject], the parent is passed an [Offset] from the
 *    screen's origin which it can add to the offsets of its children to find
 *    the [Offset] from the screen's origin to each of the children.
 *
 * Because a particular [Offset] can be interpreted as one sense at one time
 * then as the other sense at a later time, the same class is used for both
 * senses.
 *
 * See also:
 *
 *  * [Size], which represents a vector describing the size of a rectangle.
 *
 * Creates an offset. The first argument sets [x], the horizontal component,
 * and the second sets [y], the vertical component.
 */
@Suppress("NOTHING_TO_INLINE")
@kotlin.jvm.JvmInline
value class Offset internal constructor(internal val packedValue: Long) {
    val x: Float get() = unpackFloat1(packedValue)

    val y: Float get() = unpackFloat2(packedValue)

    inline operator fun component1(): Float = x

    inline operator fun component2(): Float = y

    /**
     * Returns a copy of this Offset instance optionally overriding the
     * x or y parameter
     */
    fun copy(x: Float = unpackFloat1(packedValue), y: Float = unpackFloat2(packedValue)) =
        Offset(packFloats(x, y))

    companion object {
        /**
         * An offset with zero magnitude.
         *
         * This can be used to represent the origin of a coordinate space.
         */
        val Zero = Offset(0x0L)

        /**
         * An offset with infinite x and y components.
         *
         * See also [isFinite] to check whether both components are finite.
         */
        // This is included for completeness, because [Size.infinite] exists.
        val Infinite = Offset(DualFloatInfinityBase)

        /**
         * Represents an unspecified [Offset] value, usually a replacement for `null`
         * when a primitive value is desired.
         */
        val Unspecified = Offset(UnspecifiedPackedFloats)
    }

    /**
     * Returns:
     * - False if [x] or [y] is a NaN
     * - True if [x] or [y] is infinite
     * - True otherwise
     */
    fun isValid(): Boolean {
        // Take the unsigned packed floats and see if they are < InfinityBase + 1 (first NaN)
        val v = packedValue and DualUnsignedFloatMask
        return (v - DualFirstNaN) and v.inv() and Uint64High32 == Uint64High32
    }

    /**
     * The magnitude of the offset.
     *
     * If you need this value to compare it to another [Offset]'s distance,
     * consider using [getDistanceSquared] instead, since it is cheaper to compute.
     */
    fun getDistance(): Float {
        val x = unpackFloat1(packedValue)
        val y = unpackFloat2(packedValue)
        return sqrt(x * x + y * y)
    }

    /**
     * The square of the magnitude of the offset.
     *
     * This is cheaper than computing the [getDistance] itself.
     */
    fun getDistanceSquared(): Float {
        val x = unpackFloat1(packedValue)
        val y = unpackFloat2(packedValue)
        return x * x + y * y
    }

    /**
     * Unary negation operator.
     *
     * Returns an offset with the coordinates negated.
     *
     * If the [Offset] represents an arrow on a plane, this operator returns the
     * same arrow but pointing in the reverse direction.
     */
    operator fun unaryMinus(): Offset {
        return Offset(packedValue xor DualFloatSignBit)
    }

    /**
     * Binary subtraction operator.
     *
     * Returns an offset whose [x] value is the left-hand-side operand's [x]
     * minus the right-hand-side operand's [x] and whose [y] value is the
     * left-hand-side operand's [y] minus the right-hand-side operand's [y].
     */
    operator fun minus(other: Offset): Offset {
        return Offset(
            packFloats(
                unpackFloat1(packedValue) - unpackFloat1(other.packedValue),
                unpackFloat2(packedValue) - unpackFloat2(other.packedValue)
            )
        )
    }

    /**
     * Binary addition operator.
     *
     * Returns an offset whose [x] value is the sum of the [x] values of the
     * two operands, and whose [y] value is the sum of the [y] values of the
     * two operands.
     */
    operator fun plus(other: Offset): Offset {
        return Offset(
            packFloats(
                unpackFloat1(packedValue) + unpackFloat1(other.packedValue),
                unpackFloat2(packedValue) + unpackFloat2(other.packedValue)
            )
        )
    }

    /**
     * Multiplication operator.
     *
     * Returns an offset whose coordinates are the coordinates of the
     * left-hand-side operand (an Offset) multiplied by the scalar
     * right-hand-side operand (a Float).
     */
    operator fun times(operand: Float): Offset {
        return Offset(
            packFloats(
                unpackFloat1(packedValue) * operand,
                unpackFloat2(packedValue) * operand
            )
        )
    }

    /**
     * Division operator.
     *
     * Returns an offset whose coordinates are the coordinates of the
     * left-hand-side operand (an Offset) divided by the scalar right-hand-side
     * operand (a Float).
     */
    operator fun div(operand: Float): Offset {
        return Offset(
            packFloats(
                unpackFloat1(packedValue) / operand,
                unpackFloat2(packedValue) / operand
            )
        )
    }

    /**
     * Modulo (remainder) operator.
     *
     * Returns an offset whose coordinates are the remainder of dividing the
     * coordinates of the left-hand-side operand (an Offset) by the scalar
     * right-hand-side operand (a Float).
     */
    operator fun rem(operand: Float): Offset {
        return Offset(
            packFloats(
                unpackFloat1(packedValue) % operand,
                unpackFloat2(packedValue) % operand
            )
        )
    }

    override fun toString() = if (isSpecified) {
        "Offset(${x.toStringAsFixed(1)}, ${y.toStringAsFixed(1)})"
    } else {
        // In this case reading the x or y properties will throw, and they don't contain meaningful
        // values as strings anyway.
        "Offset.Unspecified"
    }
}

/**
 * Linearly interpolate between two offsets.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid (and can
 * easily be generated by curves).
 *
 * Values for [fraction] are usually obtained from an [Animation<Float>], such as
 * an `AnimationController`.
 */
fun lerp(start: Offset, stop: Offset, fraction: Float): Offset {
    return Offset(
        packFloats(
            lerp(unpackFloat1(start.packedValue), unpackFloat1(stop.packedValue), fraction),
            lerp(unpackFloat2(start.packedValue), unpackFloat2(stop.packedValue), fraction)
        )
    )
}

/**
 * True if both x and y values of the [Offset] are finite. NaN values are not
 * considered finite.
 */
val Offset.isFinite: Boolean get() {
    // Mask out the sign bit and do an equality check in each 32-bit lane
    // against the "infinity base" mask (to check whether each packed float
    // is infinite or not).
    val v = (packedValue and DualFloatInfinityBase) xor DualFloatInfinityBase
    return (v - Uint64Low32) and v.inv() and Uint64High32 == 0L
}

/**
 * `false` when this is [Offset.Unspecified].
 */
val Offset.isSpecified: Boolean
    get() = packedValue and DualUnsignedFloatMask != UnspecifiedPackedFloats

/**
 * `true` when this is [Offset.Unspecified].
 */
val Offset.isUnspecified: Boolean
    get() = packedValue and DualUnsignedFloatMask == UnspecifiedPackedFloats

/**
 * If this [Offset]&nbsp;[isSpecified] then this is returned, otherwise [block] is executed
 * and its result is returned.
 */
inline fun Offset.takeOrElse(block: () -> Offset): Offset =
    if (isSpecified) this else block()
