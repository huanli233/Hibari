package com.huanli233.hibari.ui.unit.fontscaling

import androidx.annotation.VisibleForTesting
import java.util.Arrays
import kotlin.math.sign

class FontScaleConverterTable(
    fromSp: FloatArray,
    toDp: FloatArray
) : FontScaleConverter {
    @VisibleForTesting
    val mFromSpValues: FloatArray

    @VisibleForTesting
    val mToDpValues: FloatArray

    init {
        require(!(fromSp.size != toDp.size || fromSp.isEmpty())) {
            "Array lengths must match and be nonzero"
        }
        mFromSpValues = fromSp
        mToDpValues = toDp
    }

    override fun convertDpToSp(dp: Float): Float {
        return lookupAndInterpolate(dp, mToDpValues, mFromSpValues)
    }

    override fun convertSpToDp(sp: Float): Float {
        return lookupAndInterpolate(sp, mFromSpValues, mToDpValues)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other !is FontScaleConverterTable) return false
        return (mFromSpValues.contentEquals(other.mFromSpValues) &&
            mToDpValues.contentEquals(other.mToDpValues))
    }

    override fun hashCode(): Int {
        var result = mFromSpValues.contentHashCode()
        result = 31 * result + mToDpValues.contentHashCode()
        return result
    }

    override fun toString(): String {
        return ("FontScaleConverter{" +
            "fromSpValues=" + mFromSpValues.contentToString() +
            ", toDpValues=" + mToDpValues.contentToString() +
            '}')
    }

    companion object {
        private fun lookupAndInterpolate(
            sourceValue: Float,
            sourceValues: FloatArray,
            targetValues: FloatArray
        ): Float {
            val sourceValuePositive = Math.abs(sourceValue)
            // TODO(b/247861374): find a match at a higher index?
            val sign = sign(sourceValue)
            // We search for exact matches only, even if it's just a little off. The interpolation will
            // handle any non-exact matches.
            val index = Arrays.binarySearch(sourceValues, sourceValuePositive)
            return if (index >= 0) {
                // exact match, return the matching dp
                sign * targetValues[index]
            } else {
                // must be a value in between index and index + 1: interpolate.
                val lowerIndex = -(index + 1) - 1
                val startSp: Float
                val endSp: Float
                val startDp: Float
                val endDp: Float
                if (lowerIndex >= sourceValues.size - 1) {
                    // It's past our lookup table. Determine the last elements' scaling factor and use.
                    startSp = sourceValues[sourceValues.size - 1]
                    startDp = targetValues[sourceValues.size - 1]
                    if (startSp == 0f) return 0f
                    val scalingFactor = startDp / startSp
                    return sourceValue * scalingFactor
                } else if (lowerIndex == -1) {
                    // It's smaller than the smallest value in our table. Interpolate from 0.
                    startSp = 0f
                    startDp = 0f
                    endSp = sourceValues[0]
                    endDp = targetValues[0]
                } else {
                    startSp = sourceValues[lowerIndex]
                    endSp = sourceValues[lowerIndex + 1]
                    startDp = targetValues[lowerIndex]
                    endDp = targetValues[lowerIndex + 1]
                }
                sign * MathUtils.constrainedMap(
                    startDp,
                    endDp,
                    startSp,
                    endSp,
                    sourceValuePositive
                )
            }
        }
    }
}

object MathUtils {
    /**
     * Linearly interpolates the fraction [amount] between [start] and [stop]
     *
     * @param start starting value
     * @param stop ending value
     * @param amount normalized between 0 - 1
     */
    fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    /**
     * Inverse of [.lerp]. More precisely, returns the interpolation
     * scalar (s) that satisfies the equation:
     * `value = `[ ][.lerp]`(a, b, s)`
     *
     *
     * If `a == b`, then this function will return 0.
     */
    fun lerpInv(a: Float, b: Float, value: Float): Float {
        return if (a != b) (value - a) / (b - a) else 0.0f
    }

    /**
     * Calculates a value in [rangeMin, rangeMax] that maps value in [valueMin, valueMax] to
     * returnVal in [rangeMin, rangeMax].
     *
     *
     * Always returns a constrained value in the range [rangeMin, rangeMax], even if value is
     * outside [valueMin, valueMax].
     *
     *
     * Eg:
     * constrainedMap(0f, 100f, 0f, 1f, 0.5f) = 50f
     * constrainedMap(20f, 200f, 10f, 20f, 20f) = 200f
     * constrainedMap(20f, 200f, 10f, 20f, 50f) = 200f
     * constrainedMap(10f, 50f, 10f, 20f, 5f) = 10f
     *
     * @param rangeMin minimum of the range that should be returned.
     * @param rangeMax maximum of the range that should be returned.
     * @param valueMin minimum of range to map `value` to.
     * @param valueMax maximum of range to map `value` to.
     * @param value to map to the range [`valueMin`, `valueMax`]. Note, can be outside
     * this range, resulting in a clamped value.
     * @return the mapped value, constrained to [`rangeMin`, `rangeMax`.
     */
    fun constrainedMap(
        rangeMin: Float,
        rangeMax: Float,
        valueMin: Float,
        valueMax: Float,
        value: Float
    ): Float {
        return lerp(
            rangeMin, rangeMax,
            0f.coerceAtLeast(1f.coerceAtMost(lerpInv(valueMin, valueMax, value)))
        )
    }
}

interface FontScaleConverter {
    /**
     * Converts a dimension in "sp" to "dp".
     */
    fun convertSpToDp(sp: Float): Float

    /**
     * Converts a dimension in "dp" back to "sp".
     */
    fun convertDpToSp(dp: Float): Float
}
