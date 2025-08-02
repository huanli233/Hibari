/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huanli233.hibari.ui.graphics

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.RoundRect
import com.huanli233.hibari.ui.unit.Offset
import com.huanli233.hibari.ui.unit.Rect
import kotlin.collections.get
import kotlin.collections.set
import kotlin.text.get
import kotlin.text.set
import android.graphics.Matrix as PlatformMatrix
import android.graphics.Path as PlatformPath
import android.graphics.RectF as PlatformRectF

fun Path(): Path = AndroidPath()

/** Convert the [android.graphics.Path] instance into a Compose-compatible Path */
fun PlatformPath.asComposePath(): Path = AndroidPath(this)

/**
 * @Throws UnsupportedOperationException if this Path is not backed by an [android.graphics.Path].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Path.asAndroidPath(): PlatformPath =
    if (this is AndroidPath) {
        internalPath
    } else {
        throw UnsupportedOperationException("Unable to obtain android.graphics.Path")
    }

@Suppress("OVERRIDE_DEPRECATION") // b/407491706
/* actual */ class AndroidPath(val internalPath: PlatformPath = PlatformPath()) : Path {

    // Temporary value holders to reuse an object (not part of a state):
    private var rectF: PlatformRectF? = null
    private var radii: FloatArray? = null
    private var mMatrix: PlatformMatrix? = null

    override var fillType: PathFillType
        get() {
            return if (internalPath.fillType == PlatformPath.FillType.EVEN_ODD) {
                PathFillType.EvenOdd
            } else {
                PathFillType.NonZero
            }
        }
        set(value) {
            internalPath.fillType =
                if (value == PathFillType.EvenOdd) {
                    PlatformPath.FillType.EVEN_ODD
                } else {
                    PlatformPath.FillType.WINDING
                }
        }

    override fun moveTo(x: Float, y: Float) {
        internalPath.moveTo(x, y)
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        internalPath.rMoveTo(dx, dy)
    }

    override fun lineTo(x: Float, y: Float) {
        internalPath.lineTo(x, y)
    }

    override fun relativeLineTo(dx: Float, dy: Float) {
        internalPath.rLineTo(dx, dy)
    }

    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        internalPath.quadTo(x1, y1, x2, y2)
    }

    override fun quadraticTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        internalPath.quadTo(x1, y1, x2, y2)
    }

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        internalPath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    override fun relativeQuadraticTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        internalPath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        internalPath.cubicTo(x1, y1, x2, y2, x3, y3)
    }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float,
    ) {
        internalPath.rCubicTo(dx1, dy1, dx2, dy2, dx3, dy3)
    }

    override fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean,
    ) {
        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(left, top, right, bottom)
        internalPath.arcTo(rectF!!, startAngleDegrees, sweepAngleDegrees, forceMoveTo)
    }

    override fun addRect(@Suppress("InvalidNullabilityOverride") rect: Rect) {
        addRect(rect, Path.Direction.CounterClockwise)
    }

    override fun addRect(rect: Rect, direction: Path.Direction) {
        validateRectangle(rect)
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(rect.left, rect.top, rect.right, rect.bottom)
        internalPath.addRect(rectF!!, direction.toPlatformPathDirection())
    }

    override fun addOval(@Suppress("InvalidNullabilityOverride") oval: Rect) {
        addOval(oval, Path.Direction.CounterClockwise)
    }

    override fun addOval(oval: Rect, direction: Path.Direction) {
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(oval.left, oval.top, oval.right, oval.bottom)
        internalPath.addOval(rectF!!, direction.toPlatformPathDirection())
    }

    override fun addRoundRect(@Suppress("InvalidNullabilityOverride") roundRect: RoundRect) {
        addRoundRect(roundRect, Path.Direction.CounterClockwise)
    }

    override fun addRoundRect(roundRect: RoundRect, direction: Path.Direction) {
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(roundRect.left, roundRect.top, roundRect.right, roundRect.bottom)

        if (radii == null) radii = FloatArray(8)
        with(radii!!) {
            this[0] = roundRect.topLeftCornerRadius.x
            this[1] = roundRect.topLeftCornerRadius.y

            this[2] = roundRect.topRightCornerRadius.x
            this[3] = roundRect.topRightCornerRadius.y

            this[4] = roundRect.bottomRightCornerRadius.x
            this[5] = roundRect.bottomRightCornerRadius.y

            this[6] = roundRect.bottomLeftCornerRadius.x
            this[7] = roundRect.bottomLeftCornerRadius.y
        }
        internalPath.addRoundRect(rectF!!, radii!!, direction.toPlatformPathDirection())
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
        addArc(oval, degrees(startAngleRadians), degrees(sweepAngleRadians))
    }

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
        validateRectangle(oval)
        if (rectF == null) rectF = PlatformRectF()
        rectF!!.set(oval.left, oval.top, oval.right, oval.bottom)
        internalPath.addArc(rectF!!, startAngleDegrees, sweepAngleDegrees)
    }

    override fun addPath(path: Path, offset: Offset) {
        internalPath.addPath(path.asAndroidPath(), offset.x, offset.y)
    }

    override fun close() {
        internalPath.close()
    }

    override fun reset() {
        internalPath.reset()
    }

    override fun rewind() {
        internalPath.rewind()
    }

    override fun translate(offset: Offset) {
        if (mMatrix == null) mMatrix = PlatformMatrix() else mMatrix!!.reset()
        mMatrix!!.setTranslate(offset.x, offset.y)
        internalPath.transform(mMatrix!!)
    }

    override fun transform(matrix: Matrix) {
        if (mMatrix == null) mMatrix = PlatformMatrix()
        mMatrix!!.setFrom(matrix)
        internalPath.transform(mMatrix!!)
    }

    override fun getBounds(): Rect {
        if (rectF == null) rectF = PlatformRectF()
        with(rectF!!) {
            @Suppress("DEPRECATION") internalPath.computeBounds(this, true)
            return Rect(this.left, this.top, this.right, this.bottom)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun op(path1: Path, path2: Path, operation: PathOperation): Boolean {
        val op =
            when (operation) {
                PathOperation.Difference -> PlatformPath.Op.DIFFERENCE
                PathOperation.Intersect -> PlatformPath.Op.INTERSECT
                PathOperation.ReverseDifference -> PlatformPath.Op.REVERSE_DIFFERENCE
                PathOperation.Union -> PlatformPath.Op.UNION
                else -> PlatformPath.Op.XOR
            }
        return internalPath.op(path1.asAndroidPath(), path2.asAndroidPath(), op)
    }

    @Suppress("DEPRECATION") // Path.isConvex
    override val isConvex: Boolean
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        get() = internalPath.isConvex

    override val isEmpty: Boolean
        get() = internalPath.isEmpty

    private fun validateRectangle(rect: Rect) {
        if (rect.left.isNaN() || rect.top.isNaN() || rect.right.isNaN() || rect.bottom.isNaN()) {
            throwIllegalStateException("Invalid rectangle, make sure no value is NaN")
        }
    }
}

fun android.graphics.Matrix.setFrom(matrix: Matrix) {
    // We'll reuse the array used in Matrix to avoid allocation by temporarily
    // setting it to the 3x3 matrix used by android.graphics.Matrix
    // Store the values of the 4 x 4 matrix into temporary variables
    // to be reset after the 3 x 3 matrix is configured
    val scaleX = matrix.values[Matrix.ScaleX] // 0
    val skewY = matrix.values[Matrix.SkewY] // 1
    val v2 = matrix.values[2] // 2
    val persp0 = matrix.values[Matrix.Perspective0] // 3
    val skewX = matrix.values[Matrix.SkewX] // 4
    val scaleY = matrix.values[Matrix.ScaleY] // 5
    val v6 = matrix.values[6] // 6
    val persp1 = matrix.values[Matrix.Perspective1] // 7
    val v8 = matrix.values[8] // 8

    val translateX = matrix.values[Matrix.TranslateX]
    val translateY = matrix.values[Matrix.TranslateY]
    val persp2 = matrix.values[Matrix.Perspective2]

    val v = matrix.values

    v[android.graphics.Matrix.MSCALE_X] = scaleX
    v[android.graphics.Matrix.MSKEW_X] = skewX
    v[android.graphics.Matrix.MTRANS_X] = translateX
    v[android.graphics.Matrix.MSKEW_Y] = skewY
    v[android.graphics.Matrix.MSCALE_Y] = scaleY
    v[android.graphics.Matrix.MTRANS_Y] = translateY
    v[android.graphics.Matrix.MPERSP_0] = persp0
    v[android.graphics.Matrix.MPERSP_1] = persp1
    v[android.graphics.Matrix.MPERSP_2] = persp2

    setValues(v)

    // Reset the values back after the android.graphics.Matrix is configured
    v[Matrix.ScaleX] = scaleX // 0
    v[Matrix.SkewY] = skewY // 1
    v[2] = v2 // 2
    v[Matrix.Perspective0] = persp0 // 3
    v[Matrix.SkewX] = skewX // 4
    v[Matrix.ScaleY] = scaleY // 5
    v[6] = v6 // 6
    v[Matrix.Perspective1] = persp1 // 7
    v[8] = v8 // 8
}


internal fun throwIllegalStateException(message: String) {
    throw IllegalStateException(message)
}

private fun Path.Direction.toPlatformPathDirection() =
    when (this) {
        Path.Direction.CounterClockwise -> PlatformPath.Direction.CCW
        Path.Direction.Clockwise -> PlatformPath.Direction.CW
    }

/** Create a new path, copying the contents from the src path. */
fun Path.copy(): Path = Path().apply { addPath(this@copy) }

/* expect class */ interface Path {
    /**
     * Specifies how closed shapes (e.g. rectangles, ovals) are wound (oriented) when they are added
     * to a path.
     */
    enum class Direction {
        /** The shape is wound in counter-clockwise order. */
        CounterClockwise,
        /** The shape is wound in clockwise order. */
        Clockwise,
    }

    /**
     * Determines how the interior of this path is calculated.
     *
     * Defaults to the non-zero winding rule, [PathFillType.NonZero].
     */
    var fillType: PathFillType

    /**
     * Returns the path's convexity, as defined by the content of the path.
     *
     * A path is convex if it has a single contour, and only ever curves in a single direction.
     *
     * This function will calculate the convexity of the path from its control points, and cache the
     * result.
     */
    val isConvex: Boolean

    /**
     * Returns true if the path is empty (contains no lines or curves)
     *
     * @return true if the path is empty (contains no lines or curves)
     */
    val isEmpty: Boolean

    /** Starts a new subpath at the given coordinate */
    fun moveTo(x: Float, y: Float)

    /** Starts a new subpath at the given offset from the current point */
    fun relativeMoveTo(dx: Float, dy: Float)

    /** Adds a straight line segment from the current point to the given point */
    fun lineTo(x: Float, y: Float)

    /**
     * Adds a straight line segment from the current point to the point at the given offset from the
     * current point.
     */
    fun relativeLineTo(dx: Float, dy: Float)

    /**
     * Adds a quadratic bezier segment that curves from the current point to the given point ([x2],
     * [y2]), using the control point ([x1], [y1]).
     */
    @Deprecated(
        "Use quadraticTo() for consistency with cubicTo()",
        replaceWith = ReplaceWith("quadraticTo(x1, y1, x2, y2)"),
        level = DeprecationLevel.WARNING,
    )
    fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float)

    /**
     * Adds a quadratic bezier segment that curves from the current point to the given point ([x2],
     * [y2]), using the control point ([x1], [y1]).
     */
    fun quadraticTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        @Suppress("DEPRECATION") quadraticBezierTo(x1, y1, x2, y2)
    }

    /**
     * Adds a quadratic bezier segment that curves from the current point to the point at the offset
     * ([dx2], [dy2]) from the current point, using the control point at the offset ([dx1], [dy1])
     * from the current point.
     */
    @Deprecated(
        "Use relativeQuadraticTo() for consistency with relativeCubicTo()",
        replaceWith = ReplaceWith("relativeQuadraticTo(dx1, dy1, dx2, dy2)"),
        level = DeprecationLevel.WARNING,
    )
    fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float)

    /**
     * Adds a quadratic bezier segment that curves from the current point to the point at the offset
     * ([dx2], [dy2]) from the current point, using the control point at the offset ([dx1], [dy1])
     * from the current point.
     */
    fun relativeQuadraticTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        @Suppress("DEPRECATION") relativeQuadraticBezierTo(dx1, dy1, dx2, dy2)
    }

    /**
     * Adds a cubic bezier segment that curves from the current point to the given point ([x3],
     * [y3]), using the control points ([x1], [y1]) and ([x2], [y2]).
     */
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float)

    /**
     * Adds a cubic bezier segment that curves from the current point to the point at the offset
     * ([dx3], [dy3]) from the current point, using the control points at the offsets ([dx1], [dy1])
     * and ([dx2], [dy2]) from the current point.
     */
    fun relativeCubicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float)

    /**
     * If the [forceMoveTo] argument is false, adds a straight line segment and an arc segment.
     *
     * If the [forceMoveTo] argument is true, starts a new subpath consisting of an arc segment.
     *
     * In either case, the arc segment consists of the arc that follows the edge of the oval bounded
     * by the given rectangle, from startAngle radians around the oval up to startAngle + sweepAngle
     * radians around the oval, with zero radians being the point on the right hand side of the oval
     * that crosses the horizontal line that intersects the center of the rectangle and with
     * positive angles going clockwise around the oval.
     *
     * The line segment added if `forceMoveTo` is false starts at the current point and ends at the
     * start of the arc.
     */
    fun arcToRad(
        rect: Rect,
        startAngleRadians: Float,
        sweepAngleRadians: Float,
        forceMoveTo: Boolean,
    ) {
        arcTo(rect, degrees(startAngleRadians), degrees(sweepAngleRadians), forceMoveTo)
    }

    /**
     * If the [forceMoveTo] argument is false, adds a straight line segment and an arc segment.
     *
     * If the [forceMoveTo] argument is true, starts a new subpath consisting of an arc segment.
     *
     * In either case, the arc segment consists of the arc that follows the edge of the oval bounded
     * by the given rectangle, from startAngle degrees around the oval up to startAngle + sweepAngle
     * degrees around the oval, with zero degrees being the point on the right hand side of the oval
     * that crosses the horizontal line that intersects the center of the rectangle and with
     * positive angles going clockwise around the oval.
     *
     * The line segment added if `forceMoveTo` is false starts at the current point and ends at the
     * start of the arc.
     */
    fun arcTo(rect: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float, forceMoveTo: Boolean)

    /**
     * Adds a new subpath that consists of four lines that outline the given rectangle. The
     * rectangle is wound counter-clockwise.
     */
    @Deprecated(
        "Prefer usage of addRect() with a winding direction",
        replaceWith = ReplaceWith("addRect(rect)"),
        level = DeprecationLevel.HIDDEN,
    )
    fun addRect(rect: Rect)

    /**
     * Adds a new subpath that consists of four lines that outline the given rectangle. The
     * direction to wind the rectangle's contour is specified by [direction].
     */
    fun addRect(rect: Rect, direction: Direction = Direction.CounterClockwise)

    /**
     * Adds a new subpath that consists of a curve that forms the ellipse that fills the given
     * rectangle.
     *
     * To add a circle, pass an appropriate rectangle as `oval`. [Rect] can be used to easily
     * describe the circle's center [Offset] and radius.
     *
     * The oval is wound counter-clockwise.
     */
    @Deprecated(
        "Prefer usage of addOval() with a winding direction",
        replaceWith = ReplaceWith("addOval(oval)"),
        level = DeprecationLevel.HIDDEN,
    )
    fun addOval(oval: Rect)

    /**
     * Adds a new subpath that consists of a curve that forms the ellipse that fills the given
     * rectangle.
     *
     * To add a circle, pass an appropriate rectangle as `oval`. [Rect] can be used to easily
     * describe the circle's center [Offset] and radius.
     *
     * The direction to wind the rectangle's contour is specified by [direction].
     */
    fun addOval(oval: Rect, direction: Direction = Direction.CounterClockwise)

    /**
     * Add a round rectangle shape to the path from the given [RoundRect]. The round rectangle is
     * wound counter-clockwise.
     */
    @Deprecated(
        "Prefer usage of addRoundRect() with a winding direction",
        replaceWith = ReplaceWith("addRoundRect(roundRect)"),
        level = DeprecationLevel.HIDDEN,
    )
    fun addRoundRect(roundRect: RoundRect)

    /**
     * Add a round rectangle shape to the path from the given [RoundRect]. The direction to wind the
     * rectangle's contour is specified by [direction].
     */
    fun addRoundRect(roundRect: RoundRect, direction: Direction = Direction.CounterClockwise)

    /**
     * Adds a new subpath with one arc segment that consists of the arc that follows the edge of the
     * oval bounded by the given rectangle, from startAngle radians around the oval up to
     * startAngle + sweepAngle radians around the oval, with zero radians being the point on the
     * right hand side of the oval that crosses the horizontal line that intersects the center of
     * the rectangle and with positive angles going clockwise around the oval.
     */
    fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float)

    /**
     * Adds a new subpath with one arc segment that consists of the arc that follows the edge of the
     * oval bounded by the given rectangle, from startAngle degrees around the oval up to
     * startAngle + sweepAngle degrees around the oval, with zero degrees being the point on the
     * right hand side of the oval that crosses the horizontal line that intersects the center of
     * the rectangle and with positive angles going clockwise around the oval.
     */
    fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float)

    /** Adds a new subpath that consists of the given `path` offset by the given `offset`. */
    fun addPath(path: Path, offset: Offset = Offset.Zero)

    /**
     * Closes the last subpath, as if a straight line had been drawn from the current point to the
     * first point of the subpath.
     */
    fun close()

    /**
     * Clears the [Path] object of all subpaths, returning it to the same state it had when it was
     * created. The _current point_ is reset to the origin. This does NOT change the fill-type
     * setting.
     */
    fun reset()

    /**
     * Rewinds the path: clears any lines and curves from the path but keeps the internal data
     * structure for faster reuse.
     */
    fun rewind() {
        // Call reset to avoid AbstractMethodAdded lint API errors. Implementations are already
        // calling into the respective platform Path#rewind equivalent.
        reset()
    }

    /** Translates all the segments of every subpath by the given offset. */
    fun translate(offset: Offset)

    /** Transform the points in this path by the provided matrix */
    fun transform(matrix: Matrix) {
        // NO-OP to ensure runtime + compile time compatibility
    }

    /**
     * Compute the bounds of the control points of the path, and write the answer into bounds. If
     * the path contains 0 or 1 points, the bounds is set to (0,0,0,0)
     */
    fun getBounds(): Rect

    /**
     * Creates a new [PathIterator] for this [Path] that evaluates conics as quadratics. To preserve
     * conics, use the [Path.iterator] function that takes a [PathIterator.ConicEvaluation]
     * parameter.
     */
    operator fun iterator() = PathIterator(this)

    /**
     * Creates a new [PathIterator] for this [Path]. To preserve conics as conics (not convert them
     * to quadratics), set [conicEvaluation] to [PathIterator.ConicEvaluation.AsConic].
     *
     * @param conicEvaluation Indicates how to evaluate conic segments
     * @param tolerance When [conicEvaluation] is set to [PathIterator.ConicEvaluation.AsQuadratics]
     *   defines the maximum distance between the original conic curve and its quadratic
     *   approximations
     */
    fun iterator(conicEvaluation: PathIterator.ConicEvaluation, tolerance: Float = 0.25f) =
        PathIterator(this, conicEvaluation, tolerance)

    /**
     * Set this path to the result of applying the Op to the two specified paths. The resulting path
     * will be constructed from non-overlapping contours. The curve order is reduced where possible
     * so that cubics may be turned into quadratics, and quadratics maybe turned into lines.
     *
     * @param path1 The first operand (for difference, the minuend)
     * @param path2 The second operand (for difference, the subtrahend)
     * @param operation [PathOperation] to apply to the 2 specified paths
     * @return True if operation succeeded, false otherwise and this path remains unmodified.
     */
    fun op(path1: Path, path2: Path, operation: PathOperation): Boolean

    /** Returns the union of two paths as a new [Path]. */
    operator fun plus(path: Path) = Path().apply { op(this@Path, path, PathOperation.Union) }

    /** Returns the difference of two paths as a new [Path]. */
    operator fun minus(path: Path) = Path().apply { op(this@Path, path, PathOperation.Difference) }

    /** Returns the union of two paths as a new [Path]. */
    infix fun or(path: Path): Path = this + path

    /**
     * Returns the intersection of two paths as a new [Path]. If the paths do not intersect, returns
     * an empty path.
     */
    infix fun and(path: Path) = Path().apply { op(this@Path, path, PathOperation.Intersect) }

    /** Returns the union minus the intersection of two paths as a new [Path]. */
    infix fun xor(path: Path) = Path().apply { op(this@Path, path, PathOperation.Xor) }

    companion object {
        /**
         * Combines the two paths according to the manner specified by the given `operation`.
         *
         * The resulting path will be constructed from non-overlapping contours. The curve order is
         * reduced where possible so that cubics may be turned into quadratics, and quadratics maybe
         * turned into lines.
         *
         * Throws [IllegalArgumentException] if the combining operation fails.
         */
        fun combine(operation: PathOperation, path1: Path, path2: Path): Path {
            val path = Path()
            if (path.op(path1, path2, operation)) {
                return path
            }

            throw IllegalArgumentException(
                "Path.combine() failed.  This may be due an invalid " +
                        "path; in particular, check for NaN values."
            )
        }
    }
}