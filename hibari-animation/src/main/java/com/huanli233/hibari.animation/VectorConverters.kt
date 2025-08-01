@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package com.huanli233.hibari.animation

import com.huanli233.hibari.ui.unit.Dp
import com.huanli233.hibari.ui.unit.DpOffset
import com.huanli233.hibari.ui.unit.IntOffset
import com.huanli233.hibari.ui.unit.IntSize
import com.huanli233.hibari.ui.unit.Offset
import com.huanli233.hibari.ui.unit.Rect
import com.huanli233.hibari.ui.unit.Size
import com.huanli233.hibari.ui.unit.dp
import com.huanli233.hibari.ui.util.fastRoundToInt

/**
 * [TwoWayConverter] class contains the definition on how to convert from an arbitrary type [T] to a
 * [AnimationVector], and convert the [AnimationVector] back to the type [T]. This allows animations
 * to run on any type of objects, e.g. position, rectangle, color, etc.
 */
interface TwoWayConverter<T, V : AnimationVector> {
    /**
     * Defines how a type [T] should be converted to a Vector type (i.e. [AnimationVector1D],
     * [AnimationVector2D], [AnimationVector3D] or [AnimationVector4D], depends on the dimensions of
     * type T).
     */
    val convertToVector: (T) -> V
    /**
     * Defines how to convert a Vector type (i.e. [AnimationVector1D], [AnimationVector2D],
     * [AnimationVector3D] or [AnimationVector4D], depends on the dimensions of type T) back to type
     * [T].
     */
    val convertFromVector: (V) -> T
}

/**
 * Factory method to create a [TwoWayConverter] that converts a type [T] from and to an
 * [AnimationVector] type.
 *
 * @param convertToVector converts from type [T] to [AnimationVector]
 * @param convertFromVector converts from [AnimationVector] to type [T]
 */
fun <T, V : AnimationVector> TwoWayConverter(
    convertToVector: (T) -> V,
    convertFromVector: (V) -> T,
): TwoWayConverter<T, V> = TwoWayConverterImpl(convertToVector, convertFromVector)

/** Type converter to convert type [T] to and from a [AnimationVector1D]. */
private class TwoWayConverterImpl<T, V : AnimationVector>(
    override val convertToVector: (T) -> V,
    override val convertFromVector: (V) -> T,
) : TwoWayConverter<T, V>

internal inline fun lerp(start: Float, stop: Float, fraction: Float) =
    (start * (1 - fraction) + stop * fraction)

/** A [TwoWayConverter] that converts [Float] from and to [AnimationVector1D] */
val Float.Companion.VectorConverter: TwoWayConverter<Float, AnimationVector1D>
    get() = FloatToVector

/** A [TwoWayConverter] that converts [Int] from and to [AnimationVector1D] */
val Int.Companion.VectorConverter: TwoWayConverter<Int, AnimationVector1D>
    get() = IntToVector

private val FloatToVector: TwoWayConverter<Float, AnimationVector1D> =
    TwoWayConverter({ AnimationVector1D(it) }, { it.value })

private val IntToVector: TwoWayConverter<Int, AnimationVector1D> =
    TwoWayConverter({ AnimationVector1D(it.toFloat()) }, { it.value.toInt() })
/** A type converter that converts a [Rect] to a [AnimationVector4D], and vice versa. */
val Rect.Companion.VectorConverter: TwoWayConverter<Rect, AnimationVector4D>
    get() = RectToVector

/** A type converter that converts a [Dp] to a [AnimationVector1D], and vice versa. */
val Dp.Companion.VectorConverter: TwoWayConverter<Dp, AnimationVector1D>
    get() = DpToVector

/** A type converter that converts a [DpOffset] to a [AnimationVector2D], and vice versa. */
val DpOffset.Companion.VectorConverter: TwoWayConverter<DpOffset, AnimationVector2D>
    get() = DpOffsetToVector

/** A type converter that converts a [Size] to a [AnimationVector2D], and vice versa. */
val Size.Companion.VectorConverter: TwoWayConverter<Size, AnimationVector2D>
    get() = SizeToVector

/** A type converter that converts a [Offset] to a [AnimationVector2D], and vice versa. */
val Offset.Companion.VectorConverter: TwoWayConverter<Offset, AnimationVector2D>
    get() = OffsetToVector

/** A type converter that converts a [IntOffset] to a [AnimationVector2D], and vice versa. */
val IntOffset.Companion.VectorConverter: TwoWayConverter<IntOffset, AnimationVector2D>
    get() = IntOffsetToVector

/**
 * A type converter that converts a [IntSize] to a [AnimationVector2D], and vice versa.
 *
 * Clamps negative values to zero when converting back to [IntSize].
 */
val IntSize.Companion.VectorConverter: TwoWayConverter<IntSize, AnimationVector2D>
    get() = IntSizeToVector

/** A type converter that converts a [Dp] to a [AnimationVector1D], and vice versa. */
private val DpToVector: TwoWayConverter<Dp, AnimationVector1D> =
    TwoWayConverter(
        convertToVector = { AnimationVector1D(it.value) },
        convertFromVector = { Dp(it.value) },
    )

/** A type converter that converts a [DpOffset] to a [AnimationVector2D], and vice versa. */
private val DpOffsetToVector: TwoWayConverter<DpOffset, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.x.value, it.y.value) },
        convertFromVector = { DpOffset(it.v1.dp, it.v2.dp) },
    )

/** A type converter that converts a [Size] to a [AnimationVector2D], and vice versa. */
private val SizeToVector: TwoWayConverter<Size, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.width, it.height) },
        convertFromVector = { Size(it.v1, it.v2) },
    )

/** A type converter that converts a [Offset] to a [AnimationVector2D], and vice versa. */
private val OffsetToVector: TwoWayConverter<Offset, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.x, it.y) },
        convertFromVector = { Offset(it.v1, it.v2) },
    )

/** A type converter that converts a [IntOffset] to a [AnimationVector2D], and vice versa. */
private val IntOffsetToVector: TwoWayConverter<IntOffset, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.x.toFloat(), it.y.toFloat()) },
        convertFromVector = { IntOffset(it.v1.fastRoundToInt(), it.v2.fastRoundToInt()) },
    )

/**
 * A type converter that converts a [IntSize] to a [AnimationVector2D], and vice versa.
 *
 * Clamps negative values to zero when converting back to [IntSize].
 */
private val IntSizeToVector: TwoWayConverter<IntSize, AnimationVector2D> =
    TwoWayConverter(
        { AnimationVector2D(it.width.toFloat(), it.height.toFloat()) },
        {
            IntSize(
                width = it.v1.fastRoundToInt().fastCoerceAtLeast(0),
                height = it.v2.fastRoundToInt().fastCoerceAtLeast(0),
            )
        },
    )

inline fun Int.fastCoerceAtLeast(minimumValue: Int): Int {
    return if (this < minimumValue) minimumValue else this
}

/** A type converter that converts a [Rect] to a [AnimationVector4D], and vice versa. */
private val RectToVector: TwoWayConverter<Rect, AnimationVector4D> =
    TwoWayConverter(
        convertToVector = { AnimationVector4D(it.left, it.top, it.right, it.bottom) },
        convertFromVector = { Rect(it.v1, it.v2, it.v3, it.v4) },
    )

private inline fun Float.fastCoerceAtLeast(minimumValue: Float): Float {
    return if (this < minimumValue) minimumValue else this
}

/** Ensures that this value is not greater than the specified [maximumValue]. */
private inline fun Float.fastCoerceAtMost(maximumValue: Float): Float {
    return if (this > maximumValue) maximumValue else this
}