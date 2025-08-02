package com.huanli233.hibari.ui.graphics

import androidx.core.os.BuildCompat
import androidx.graphics.path.PathIterator as PlatformPathIterator
import androidx.graphics.path.PathIterator.ConicEvaluation as PlatformConicEvaluation
import androidx.graphics.path.PathSegment.Type as PlatformPathSegmentType

fun PathIterator(
    path: Path,
    conicEvaluation: PathIterator.ConicEvaluation = PathIterator.ConicEvaluation.AsQuadratics,
    tolerance: Float = 0.25f
): PathIterator = AndroidPathIterator(path, conicEvaluation, tolerance)

@OptIn(BuildCompat.PrereleaseSdkCheck::class)
private class AndroidPathIterator(
    override val path: Path,
    override val conicEvaluation: PathIterator.ConicEvaluation,
    override val tolerance: Float,
) : PathIterator {
    private val segmentPoints = FloatArray(8)

    private val implementation =
        PlatformPathIterator(
            path.asAndroidPath(),
            when (conicEvaluation) {
                PathIterator.ConicEvaluation.AsConic -> PlatformConicEvaluation.AsConic
                PathIterator.ConicEvaluation.AsQuadratics -> PlatformConicEvaluation.AsQuadratics
            },
            tolerance,
        )

    override fun calculateSize(includeConvertedConics: Boolean): Int =
        implementation.calculateSize(includeConvertedConics)

    override fun hasNext(): Boolean = implementation.hasNext()

    override fun next(outPoints: FloatArray, offset: Int): PathSegment.Type =
        implementation.next(outPoints, offset).toPathSegmentType()

    override fun next(): PathSegment {
        val p = segmentPoints
        // Compiler hint to bypass bound checks
        if (p.size < 8) return DoneSegment

        val type = implementation.next(p, 0).toPathSegmentType()
        if (type == PathSegment.Type.Done) return DoneSegment
        if (type == PathSegment.Type.Close) return CloseSegment

        val points =
            when (type) {
                PathSegment.Type.Move -> floatArrayOf(p[0], p[1])
                PathSegment.Type.Line -> floatArrayOf(p[0], p[1], p[2], p[3])
                PathSegment.Type.Quadratic -> floatArrayOf(p[0], p[1], p[2], p[3], p[4], p[5])
                PathSegment.Type.Conic -> floatArrayOf(p[0], p[1], p[2], p[3], p[4], p[5])
                PathSegment.Type.Cubic ->
                    floatArrayOf(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7])
                else -> FloatArray(0) // Unreachable since we tested for Done and Close above
            }

        return PathSegment(type, points, if (type == PathSegment.Type.Conic) p[6] else 0.0f)
    }
}

private fun PlatformPathSegmentType.toPathSegmentType() =
    when (this) {
        PlatformPathSegmentType.Move -> PathSegment.Type.Move
        PlatformPathSegmentType.Line -> PathSegment.Type.Line
        PlatformPathSegmentType.Quadratic -> PathSegment.Type.Quadratic
        PlatformPathSegmentType.Conic -> PathSegment.Type.Conic
        PlatformPathSegmentType.Cubic -> PathSegment.Type.Cubic
        PlatformPathSegmentType.Close -> PathSegment.Type.Close
        PlatformPathSegmentType.Done -> PathSegment.Type.Done
    }

/**
 * A path iterator can be used to iterate over all the [segments][PathSegment] that make up a path.
 * Those segments may in turn define multiple contours inside the path.
 *
 * The handling of conic segments is defined by the [conicEvaluation] property. When set to
 * [AsConic][ConicEvaluation.AsConic], conic segments are preserved, but when set to
 * [AsConic][ConicEvaluation.AsQuadratics], conic segments are approximated using 1 or more
 * quadratic segments. The error of the approximation is controlled by [tolerance].
 */
interface PathIterator : Iterator<PathSegment> {
    /**
     * Used to define how conic segments are evaluated when iterating over a [Path] using
     * [PathIterator].
     */
    enum class ConicEvaluation {
        /** Conic segments are returned as conic segments. */
        AsConic,

        /**
         * Conic segments are returned as quadratic approximations. The quality of the approximation
         * is defined by a tolerance value.
         */
        AsQuadratics,
    }

    /** The [Path] this iterator iterates on. */
    val path: Path

    /**
     * Indicates whether conic segments, when present, are preserved as-is or converted to quadratic
     * segments, using an approximation whose error is controlled by [tolerance].
     */
    val conicEvaluation: ConicEvaluation

    /**
     * Error of the approximation used to evaluate conic segments if they are converted to
     * quadratics. The error is defined as the maximum distance between the original conic segment
     * and its quadratic approximation. See [conicEvaluation].
     */
    val tolerance: Float

    /**
     * Returns the number of verbs present in this iterator, i.e. the number of calls to [next]
     * required to complete the iteration.
     *
     * By default, [calculateSize] returns the true number of operations in the iterator. Deriving
     * this result requires converting any conics to quadratics, if [conicEvaluation] is set to
     * [ConicEvaluation.AsQuadratics], which takes extra processing time. Set
     * [includeConvertedConics] to false if an approximate size, not including conic conversion, is
     * sufficient.
     *
     * @param includeConvertedConics The returned size includes any required conic conversions.
     *   Default is true, so it will return the exact size, at the cost of iterating through all
     *   elements and converting any conics as appropriate. Set to false to save on processing, at
     *   the cost of a less exact result.
     */
    fun calculateSize(includeConvertedConics: Boolean = true): Int

    /** Returns `true` if the iteration has more elements. */
    override fun hasNext(): Boolean

    /**
     * Returns the [type][PathSegment.Type] of the next [path segment][PathSegment] in the iteration
     * and fills [outPoints] with the points specific to the segment type. Each pair of floats in
     * the [outPoints] array represents a point for the given segment. The number of pairs of floats
     * depends on the [PathSegment.Type]:
     * - [Move][PathSegment.Type.Move]: 1 pair (indices 0 to 1)
     * - [Line][PathSegment.Type.Line]: 2 pairs (indices 0 to 3)
     * - [Quadratic][PathSegment.Type.Quadratic]: 3 pairs (indices 0 to 5)
     * - [Conic][PathSegment.Type.Conic]: 3 pairs (indices 0 to 5), and the conic
     *   [weight][PathSegment.weight] at index 6. The value of the last float is undefined. See
     *   [PathSegment.Type.Conic] for more details
     * - [Cubic][PathSegment.Type.Cubic]: 4 pairs (indices 0 to 7)
     * - [Close][PathSegment.Type.Close]: 0 pair
     * - [Done][PathSegment.Type.Done]: 0 pair
     *
     * This method does not allocate any memory.
     *
     * @param outPoints A [FloatArray] large enough to hold 8 floats starting at [offset], throws an
     *   [IllegalStateException] otherwise.
     * @param offset Offset in [outPoints] where to store the result
     */
    fun next(outPoints: FloatArray, offset: Int = 0): PathSegment.Type

    /**
     * Returns the next [path segment][PathSegment] in the iteration, or [DoneSegment] if the
     * iteration is finished. To save on allocations, use the alternative [next] function, which
     * takes a [FloatArray].
     */
    override fun next(): PathSegment
}