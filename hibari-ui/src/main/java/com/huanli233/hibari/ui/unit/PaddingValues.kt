package com.huanli233.hibari.ui.unit

import kotlin.compareTo
import kotlin.hashCode

data class StablePaddingValues(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp
) {
    companion object {
        fun fromPaddingValues(paddingValues: PaddingValues): StablePaddingValues {
            return StablePaddingValues(
                start = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
                top = paddingValues.calculateTopPadding(),
                end = paddingValues.calculateRightPadding(LayoutDirection.Ltr),
                bottom = paddingValues.calculateBottomPadding()
            )
        }
    }
}

interface PaddingValues {
    /**
     * The padding to be applied along the left edge inside a box.
     */
    fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp
    /**
     * The padding to be applied along the top edge inside a box.
     */
    fun calculateTopPadding(): Dp
    /**
     * The padding to be applied along the right edge inside a box.
     */
    fun calculateRightPadding(layoutDirection: LayoutDirection): Dp
    /**
     * The padding to be applied along the bottom edge inside a box.
     */
    fun calculateBottomPadding(): Dp

    /**
     * Describes an absolute (RTL unaware) padding to be applied along the edges inside a box.
     */
    class Absolute(
        private val left: Dp = 0.dp,
        private val top: Dp = 0.dp,
        private val right: Dp = 0.dp,
        private val bottom: Dp = 0.dp
    ) : PaddingValues {

        init {
            require(left.value >= 0) { "Left padding must be non-negative" }
            require(top.value >= 0) { "Top padding must be non-negative" }
            require(right.value >= 0) { "Right padding must be non-negative" }
            require(bottom.value >= 0) { "Bottom padding must be non-negative" }
        }

        override fun calculateLeftPadding(layoutDirection: LayoutDirection) = left

        override fun calculateTopPadding() = top

        override fun calculateRightPadding(layoutDirection: LayoutDirection) = right

        override fun calculateBottomPadding() = bottom

        override fun equals(other: Any?): Boolean {
            if (other !is Absolute) return false
            return left == other.left &&
                    top == other.top &&
                    right == other.right &&
                    bottom == other.bottom
        }

        override fun hashCode() =
            ((left.hashCode() * 31 + top.hashCode()) * 31 + right.hashCode()) *
                    31 + bottom.hashCode()

        override fun toString() =
            "PaddingValues.Absolute(left=$left, top=$top, right=$right, bottom=$bottom)"
    }
}

/**
 * The padding to be applied along the start edge inside a box: along the left edge if
 * the layout direction is LTR, or along the right edge for RTL.
 */
fun PaddingValues.calculateStartPadding(layoutDirection: LayoutDirection) =
    if (layoutDirection == LayoutDirection.Ltr) {
        calculateLeftPadding(layoutDirection)
    } else {
        calculateRightPadding(layoutDirection)
    }

/**
 * The padding to be applied along the end edge inside a box: along the right edge if
 * the layout direction is LTR, or along the left edge for RTL.
 */
fun PaddingValues.calculateEndPadding(layoutDirection: LayoutDirection) =
    if (layoutDirection == LayoutDirection.Ltr) {
        calculateRightPadding(layoutDirection)
    } else {
        calculateLeftPadding(layoutDirection)
    }

/**
 * Creates a padding of [all] dp along all 4 edges.
 */
fun PaddingValues(all: Dp): PaddingValues = PaddingValuesImpl(all, all, all, all)

/**
 * Creates a padding of [horizontal] dp along the left and right edges, and of [vertical]
 * dp along the top and bottom edges.
 */
fun PaddingValues(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): PaddingValues =
    PaddingValuesImpl(horizontal, vertical, horizontal, vertical)

/**
 * Creates a padding to be applied along the edges inside a box. In LTR contexts [start] will
 * be applied along the left edge and [end] will be applied along the right edge. In RTL contexts,
 * [start] will correspond to the right edge and [end] to the left.
 */
fun PaddingValues(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
): PaddingValues = PaddingValuesImpl(start, top, end, bottom)

internal class PaddingValuesImpl(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp
) : PaddingValues {

    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Ltr) start else end

    override fun calculateTopPadding() = top

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        if (layoutDirection == LayoutDirection.Ltr) end else start

    override fun calculateBottomPadding() = bottom

    override fun equals(other: Any?): Boolean {
        if (other !is PaddingValuesImpl) return false
        return start == other.start &&
                top == other.top &&
                end == other.end &&
                bottom == other.bottom
    }

    override fun hashCode() =
        ((start.hashCode() * 31 + top.hashCode()) * 31 + end.hashCode()) * 31 + bottom.hashCode()

    override fun toString() = "PaddingValues(start=$start, top=$top, end=$end, bottom=$bottom)"
}