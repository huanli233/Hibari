package com.huanli233.hibari.foundation.layout

import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.captionBar
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.mandatorySystemGestures
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.WindowInsetsCompat.Type.systemGestures
import androidx.core.view.WindowInsetsCompat.Type.tappableElement
import com.huanli233.hibari.foundation.layout.WindowInsetsSides.Companion.Bottom
import com.huanli233.hibari.foundation.layout.WindowInsetsSides.Companion.End
import com.huanli233.hibari.foundation.layout.WindowInsetsSides.Companion.Left
import com.huanli233.hibari.foundation.layout.WindowInsetsSides.Companion.Right
import com.huanli233.hibari.foundation.layout.WindowInsetsSides.Companion.Start
import com.huanli233.hibari.foundation.layout.WindowInsetsSides.Companion.Top
import com.huanli233.hibari.runtime.R
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.currentView
import com.huanli233.hibari.runtime.effects.DisposableEffect
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.locals.LocalDensity
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.runtime.snapshots.Snapshot
import com.huanli233.hibari.ui.unit.Density
import com.huanli233.hibari.ui.unit.Dp
import com.huanli233.hibari.ui.unit.LayoutDirection
import com.huanli233.hibari.ui.unit.PaddingValues
import com.huanli233.hibari.ui.unit.dp
import org.jetbrains.annotations.TestOnly
import java.util.WeakHashMap
import androidx.core.graphics.Insets as AndroidXInsets


interface WindowInsets {
    /**
     * The space, in pixels, at the left of the window that the inset represents.
     */
    fun getLeft(density: Density, layoutDirection: LayoutDirection): Int

    /**
     * The space, in pixels, at the top of the window that the inset represents.
     */
    fun getTop(density: Density): Int

    /**
     * The space, in pixels, at the right of the window that the inset represents.
     */
    fun getRight(density: Density, layoutDirection: LayoutDirection): Int

    /**
     * The space, in pixels, at the bottom of the window that the inset represents.
     */
    fun getBottom(density: Density): Int

    companion object
}

/**
 * A [WindowInsets] whose values can change without changing the instance. This is useful
 * to avoid recomposition when [WindowInsets] can change.
 *
 * @sample androidx.compose.foundation.layout.samples.withConsumedInsetsSample
 *
 * Note: This API as experimental since it doesn't enforce the right consumption patterns.
 */
class MutableWindowInsets(initialInsets: WindowInsets = WindowInsets(0, 0, 0, 0)) : WindowInsets {
    /**
     * The [WindowInsets] that are used for [left][getLeft], [top][getTop], [right][getRight],
     * and [bottom][getBottom] values.
     */
    var insets by mutableStateOf(initialInsets)

    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int =
        insets.getLeft(density, layoutDirection)

    override fun getTop(density: Density): Int = insets.getTop(density)

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int =
        insets.getRight(density, layoutDirection)

    override fun getBottom(density: Density): Int = insets.getBottom(density)
}

/**
 * [WindowInsetsSides] is used in [WindowInsets.only] to define which sides of the
 * [WindowInsets] should apply.
 */
@kotlin.jvm.JvmInline
value class WindowInsetsSides private constructor(private val value: Int) {
    /**
     * Returns a [WindowInsetsSides] containing sides defied in [sides] and the
     * sides in `this`.
     */
    operator fun plus(sides: WindowInsetsSides): WindowInsetsSides =
        WindowInsetsSides(value or sides.value)

    internal fun hasAny(sides: WindowInsetsSides): Boolean =
        (value and sides.value) != 0

    override fun toString(): String = "WindowInsetsSides(${valueToString()})"

    private fun valueToString(): String = buildString {
        fun appendPlus(text: String) {
            if (isNotEmpty()) append('+')
            append(text)
        }

        if (value and Start.value == Start.value) appendPlus("Start")
        if (value and Left.value == Left.value) appendPlus("Left")
        if (value and Top.value == Top.value) appendPlus("Top")
        if (value and End.value == End.value) appendPlus("End")
        if (value and Right.value == Right.value) appendPlus("Right")
        if (value and Bottom.value == Bottom.value) appendPlus("Bottom")
    }

    companion object {
        //     _---- allowLeft  in ltr
        //    /
        //    | _--- allowRight in ltr
        //    |/
        //    || _-- allowLeft  in rtl
        //    ||/
        //    ||| _- allowRight in rtl
        //    |||/
        //    VVVV
        //    Mask   = ----
        //
        //    Left   = 1010
        //    Right  = 0101
        //    Start  = 1001
        //    End    = 0110

        internal val AllowLeftInLtr = WindowInsetsSides(1 shl 3)
        internal val AllowRightInLtr = WindowInsetsSides(1 shl 2)
        internal val AllowLeftInRtl = WindowInsetsSides(1 shl 1)
        internal val AllowRightInRtl = WindowInsetsSides(1 shl 0)

        /**
         * Indicates a [WindowInsets] start side, which is left or right
         * depending on [LayoutDirection]. If [LayoutDirection.Ltr], [Start]
         * is the left side. If [LayoutDirection.Rtl], [Start] is the right side.
         *
         * Use [Left] or [Right] if the physical direction is required.
         */
        val Start = AllowLeftInLtr + AllowRightInRtl

        /**
         * Indicates a [WindowInsets] end side, which is left or right
         * depending on [LayoutDirection]. If [LayoutDirection.Ltr], [End]
         * is the right side. If [LayoutDirection.Rtl], [End] is the left side.
         *
         * Use [Left] or [Right] if the physical direction is required.
         */
        val End = AllowRightInLtr + AllowLeftInRtl

        /**
         * Indicates a [WindowInsets] top side.
         */
        val Top = WindowInsetsSides(1 shl 4)

        /**
         * Indicates a [WindowInsets] bottom side.
         */
        val Bottom = WindowInsetsSides(1 shl 5)

        /**
         * Indicates a [WindowInsets] left side. Most layouts will prefer using
         * [Start] or [End] to account for [LayoutDirection].
         */
        val Left = AllowLeftInLtr + AllowLeftInRtl

        /**
         * Indicates a [WindowInsets] right side. Most layouts will prefer using
         * [Start] or [End] to account for [LayoutDirection].
         */
        val Right = AllowRightInLtr + AllowRightInRtl

        /**
         * Indicates a [WindowInsets] horizontal sides. This is a combination of
         * [Left] and [Right] sides, or [Start] and [End] sides.
         */
        val Horizontal = Left + Right

        /**
         * Indicates a [WindowInsets] [Top] and [Bottom] sides.
         */
        val Vertical = Top + Bottom
    }
}

/**
 * Returns a [WindowInsets] that has the maximum values of this [WindowInsets] and [insets].
 */
fun WindowInsets.union(insets: WindowInsets): WindowInsets = UnionInsets(this, insets)

/**
 * Returns the values in this [WindowInsets] that are not also in [insets]. For example, if this
 * [WindowInsets] has a [WindowInsets.getTop] value of `10` and [insets] has a
 * [WindowInsets.getTop] value of `8`, the returned [WindowInsets] will have a
 * [WindowInsets.getTop] value of `2`.
 *
 * Negative values are never returned. For example if [insets] has a [WindowInsets.getTop] of `10`
 * and this has a [WindowInsets.getTop] of `0`, the returned [WindowInsets] will have a
 * [WindowInsets.getTop] value of `0`.
 */
fun WindowInsets.exclude(insets: WindowInsets): WindowInsets = ExcludeInsets(this, insets)

/**
 * Returns a [WindowInsets] that has values of this, added to the values of [insets].
 * For example, if this has a top of 10 and insets has a top of 5, the returned [WindowInsets]
 * will have a top of 15.
 */
fun WindowInsets.add(insets: WindowInsets): WindowInsets = AddedInsets(this, insets)

/**
 * Returns a [WindowInsets] that eliminates all dimensions except the ones that are enabled.
 * For example, to have a [WindowInsets] at the bottom of the screen, pass
 * [WindowInsetsSides.Bottom].
 */
fun WindowInsets.only(sides: WindowInsetsSides): WindowInsets = LimitInsets(this, sides)

/**
 * Convert a [WindowInsets] to a [PaddingValues] and uses [LocalDensity] for DP to pixel
 * conversion. [PaddingValues] can be passed to some containers to pad internal content so that
 * it doesn't overlap the insets when fully scrolled. Ensure that the insets are
 * [consumed][consumeWindowInsets] after the padding is applied if insets are to be used further
 * down the hierarchy.
 *
 * @sample androidx.compose.foundation.layout.samples.paddingValuesSample
 */
@Tunable
fun WindowInsets.asPaddingValues(): PaddingValues = InsetsPaddingValues(this, LocalDensity.current)

/**
 * Convert a [WindowInsets] to a [PaddingValues] and uses [density] for DP to pixel conversion.
 * [PaddingValues] can be passed to some containers to pad internal content so that it doesn't
 * overlap the insets when fully scrolled. Ensure that the insets are
 * [consumed][consumeWindowInsets] after the padding is applied if insets are to be used further
 * down the hierarchy.
 *
 * @sample androidx.compose.foundation.layout.samples.paddingValuesSample
 */
fun WindowInsets.asPaddingValues(density: Density): PaddingValues =
    InsetsPaddingValues(this, density)

/**
 * Convert a [PaddingValues] to a [WindowInsets].
 */
internal fun PaddingValues.asInsets(): WindowInsets = PaddingValuesInsets(this)

/**
 * Create a [WindowInsets] with fixed dimensions.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsInt
 */
fun WindowInsets(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): WindowInsets =
    FixedIntInsets(left, top, right, bottom)

/**
 * Create a [WindowInsets] with fixed dimensions, using [Dp] values.
 *
 * @sample androidx.compose.foundation.layout.samples.insetsDp
 */
fun WindowInsets(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp
): WindowInsets = FixedDpInsets(left, top, right, bottom)

private class FixedIntInsets(
    private val leftVal: Int,
    private val topVal: Int,
    private val rightVal: Int,
    private val bottomVal: Int
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = leftVal
    override fun getTop(density: Density): Int = topVal
    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = rightVal
    override fun getBottom(density: Density): Int = bottomVal

    override fun toString(): String {
        return "Insets(left=$leftVal, top=$topVal, right=$rightVal, bottom=$bottomVal)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FixedIntInsets) {
            return false
        }

        return leftVal == other.leftVal && topVal == other.topVal &&
                rightVal == other.rightVal && bottomVal == other.bottomVal
    }

    override fun hashCode(): Int {
        var result = leftVal
        result = 31 * result + topVal
        result = 31 * result + rightVal
        result = 31 * result + bottomVal
        return result
    }
}

private class FixedDpInsets(
    private val leftDp: Dp,
    private val topDp: Dp,
    private val rightDp: Dp,
    private val bottomDp: Dp
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
        with(density) { leftDp.roundToPx() }

    override fun getTop(density: Density) = with(density) { topDp.roundToPx() }
    override fun getRight(density: Density, layoutDirection: LayoutDirection) =
        with(density) { rightDp.roundToPx() }
    override fun getBottom(density: Density) = with(density) { bottomDp.roundToPx() }

    override fun toString(): String {
        return "Insets(left=$leftDp, top=$topDp, right=$rightDp, bottom=$bottomDp)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FixedDpInsets) {
            return false
        }

        return leftDp == other.leftDp && topDp == other.topDp &&
                rightDp == other.rightDp && bottomDp == other.bottomDp
    }

    override fun hashCode(): Int {
        var result = leftDp.hashCode()
        result = 31 * result + topDp.hashCode()
        result = 31 * result + rightDp.hashCode()
        result = 31 * result + bottomDp.hashCode()
        return result
    }
}

/**
 * An [WindowInsets] that comes straight from [androidx.core.graphics.Insets], whose value can
 * be updated.
 */
internal class ValueInsets(insets: InsetsValues, val name: String) : WindowInsets {
    internal var value by mutableStateOf(insets)

    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = value.left
    override fun getTop(density: Density) = value.top
    override fun getRight(density: Density, layoutDirection: LayoutDirection) = value.right
    override fun getBottom(density: Density) = value.bottom

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is ValueInsets) {
            return false
        }
        return value == other.value
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "$name(left=${value.left}, top=${value.top}, " +
                "right=${value.right}, bottom=${value.bottom})"
    }
}

internal class InsetsValues(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InsetsValues) {
            return false
        }

        return left == other.left &&
                top == other.top &&
                right == other.right &&
                bottom == other.bottom
    }

    override fun hashCode(): Int {
        var result = left
        result = 31 * result + top
        result = 31 * result + right
        result = 31 * result + bottom
        return result
    }

    override fun toString(): String =
        "InsetsValues(left=$left, top=$top, right=$right, bottom=$bottom)"
}

/**
 * An [WindowInsets] that includes the maximum value of [first] and [second] as returned from
 * [WindowInsets.union].
 */
private class UnionInsets(
    private val first: WindowInsets,
    private val second: WindowInsets
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
        maxOf(first.getLeft(density, layoutDirection), second.getLeft(density, layoutDirection))

    override fun getTop(density: Density) =
        maxOf(first.getTop(density), second.getTop(density))

    override fun getRight(density: Density, layoutDirection: LayoutDirection) =
        maxOf(first.getRight(density, layoutDirection), second.getRight(density, layoutDirection))

    override fun getBottom(density: Density) =
        maxOf(first.getBottom(density), second.getBottom(density))

    override fun hashCode(): Int = first.hashCode() + second.hashCode() * 31

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is UnionInsets) {
            return false
        }
        return other.first == first && other.second == second
    }

    override fun toString(): String = "($first ∪ $second)"
}

/**
 * An [WindowInsets] that includes the added value of [first] to [second].
 */
private class AddedInsets(
    private val first: WindowInsets,
    private val second: WindowInsets
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
        first.getLeft(density, layoutDirection) + second.getLeft(density, layoutDirection)

    override fun getTop(density: Density) =
        first.getTop(density) + second.getTop(density)

    override fun getRight(density: Density, layoutDirection: LayoutDirection) =
        first.getRight(density, layoutDirection) + second.getRight(density, layoutDirection)

    override fun getBottom(density: Density) =
        first.getBottom(density) + second.getBottom(density)

    override fun hashCode(): Int = first.hashCode() + second.hashCode() * 31

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is AddedInsets) {
            return false
        }
        return other.first == first && other.second == second
    }

    override fun toString(): String = "($first + $second)"
}

/**
 * An [WindowInsets] that includes the value of [included] that is not included in [excluded] as
 * returned from [WindowInsets.exclude].
 */
private class ExcludeInsets(
    private val included: WindowInsets,
    private val excluded: WindowInsets
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
        (included.getLeft(density, layoutDirection) - excluded.getLeft(density, layoutDirection))
            .coerceAtLeast(0)

    override fun getTop(density: Density) =
        (included.getTop(density) - excluded.getTop(density)).coerceAtLeast(0)

    override fun getRight(density: Density, layoutDirection: LayoutDirection) =
        (included.getRight(density, layoutDirection) - excluded.getRight(density, layoutDirection))
            .coerceAtLeast(0)

    override fun getBottom(density: Density) =
        (included.getBottom(density) - excluded.getBottom(density)).coerceAtLeast(0)

    override fun toString(): String = "($included - $excluded)"

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ExcludeInsets) {
            return false
        }

        return (other.included == included && other.excluded == excluded)
    }

    override fun hashCode(): Int = 31 * included.hashCode() + excluded.hashCode()
}

/**
 * An [WindowInsets] calculated from [paddingValues].
 */
private class PaddingValuesInsets(private val paddingValues: PaddingValues) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) = with(density) {
        paddingValues.calculateLeftPadding(layoutDirection).roundToPx()
    }

    override fun getTop(density: Density) = with(density) {
        paddingValues.calculateTopPadding().roundToPx()
    }

    override fun getRight(density: Density, layoutDirection: LayoutDirection) = with(density) {
        paddingValues.calculateRightPadding(layoutDirection).roundToPx()
    }

    override fun getBottom(density: Density) = with(density) {
        paddingValues.calculateBottomPadding().roundToPx()
    }

    override fun toString(): String {
        val layoutDirection = LayoutDirection.Ltr
        val start = paddingValues.calculateLeftPadding(layoutDirection)
        val top = paddingValues.calculateTopPadding()
        val end = paddingValues.calculateRightPadding(layoutDirection)
        val bottom = paddingValues.calculateBottomPadding()
        return "PaddingValues($start, $top, $end, $bottom)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is PaddingValuesInsets) {
            return false
        }

        return other.paddingValues == paddingValues
    }

    override fun hashCode(): Int = paddingValues.hashCode()
}

private class LimitInsets(
    val insets: WindowInsets,
    val sides: WindowInsetsSides
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int {
        val layoutDirectionSide = if (layoutDirection == LayoutDirection.Ltr) {
            WindowInsetsSides.AllowLeftInLtr
        } else {
            WindowInsetsSides.AllowLeftInRtl
        }
        val allowLeft = sides.hasAny(layoutDirectionSide)
        return if (allowLeft) {
            insets.getLeft(density, layoutDirection)
        } else {
            0
        }
    }

    override fun getTop(density: Density): Int =
        if (sides.hasAny(WindowInsetsSides.Top)) insets.getTop(density) else 0

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int {
        val layoutDirectionSide = if (layoutDirection == LayoutDirection.Ltr) {
            WindowInsetsSides.AllowRightInLtr
        } else {
            WindowInsetsSides.AllowRightInRtl
        }
        val allowRight = sides.hasAny(layoutDirectionSide)
        return if (allowRight) {
            insets.getRight(density, layoutDirection)
        } else {
            0
        }
    }

    override fun getBottom(density: Density): Int =
        if (sides.hasAny(WindowInsetsSides.Bottom)) insets.getBottom(density) else 0

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is LimitInsets) {
            return false
        }
        return insets == other.insets &&
                sides == other.sides
    }

    override fun hashCode(): Int {
        var result = insets.hashCode()
        result = 31 * result + sides.hashCode()
        return result
    }

    override fun toString(): String = "($insets only $sides)"
}

private class InsetsPaddingValues(
    val insets: WindowInsets,
    private val density: Density
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) = with(density) {
        insets.getLeft(this, layoutDirection).toDp()
    }

    override fun calculateTopPadding() = with(density) {
        insets.getTop(this).toDp()
    }

    override fun calculateRightPadding(layoutDirection: LayoutDirection) = with(density) {
        insets.getRight(this, layoutDirection).toDp()
    }

    override fun calculateBottomPadding() = with(density) {
        insets.getBottom(this).toDp()
    }

    override fun toString(): String {
        return "InsetsPaddingValues(insets=$insets, density=$density)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is InsetsPaddingValues) {
            return false
        }
        return insets == other.insets && density == other.density
    }

    override fun hashCode(): Int {
        var result = insets.hashCode()
        result = 31 * result + density.hashCode()
        return result
    }
}

internal fun AndroidXInsets.toInsetsValues(): InsetsValues =
    InsetsValues(left, top, right, bottom)

internal fun ValueInsets(insets: AndroidXInsets, name: String): ValueInsets =
    ValueInsets(insets.toInsetsValues(), name)

internal class AndroidWindowInsets(
    internal val type: Int,
    private val name: String
) : WindowInsets {
    internal var insets by mutableStateOf(AndroidXInsets.NONE)

    /**
     * Returns whether the insets are visible, irrespective of whether or not they
     * intersect with the Window.
     */
    var isVisible by mutableStateOf(true)
        private set

    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int {
        return insets.left
    }

    override fun getTop(density: Density): Int {
        return insets.top
    }

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int {
        return insets.right
    }

    override fun getBottom(density: Density): Int {
        return insets.bottom
    }

    internal fun update(windowInsetsCompat: WindowInsetsCompat, typeMask: Int) {
        if (typeMask == 0 || typeMask and type != 0) {
            insets = windowInsetsCompat.getInsets(type)
            isVisible = windowInsetsCompat.isVisible(type)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AndroidWindowInsets) return false

        return type == other.type && insets == other.insets
    }

    override fun hashCode(): Int {
        return type
    }

    override fun toString(): String {
        return "$name(${insets.left}, ${insets.top}, ${insets.right}, ${insets.bottom})"
    }
}

/**
 * For the [WindowInsetsCompat.Type.captionBar].
 */
val WindowInsets.Companion.captionBar: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().captionBar

/**
 * For the [WindowInsetsCompat.Type.displayCutout]. This insets represents the area that the
 * display cutout (e.g. for camera) is and important content should be excluded from.
 */
val WindowInsets.Companion.displayCutout: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().displayCutout

/**
 * For the [WindowInsetsCompat.Type.ime]. On API level 23 (M) and above, the soft keyboard can be
 * detected and [ime] will update when it shows. On API 30 (R) and above, the [ime] insets will
 * animate synchronously with the IME animation.
 *
 * Developers should set `android:windowSoftInputMode="adjustResize"` in their
 * `AndroidManifest.xml` file and call `WindowCompat.setDecorFitsSystemWindows(window, false)`
 * in their [android.app.Activity.onCreate].
 */
val WindowInsets.Companion.ime: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().ime

/**
 * For the [WindowInsetsCompat.Type.mandatorySystemGestures]. These insets represents the
 * space where system gestures have priority over application gestures.
 */
val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().mandatorySystemGestures

/**
 * For the [WindowInsetsCompat.Type.navigationBars]. These insets represent where
 * system UI places navigation bars. Interactive UI should avoid the navigation bars
 * area.
 */
val WindowInsets.Companion.navigationBars: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().navigationBars

/**
 * For the [WindowInsetsCompat.Type.statusBars].
 */
val WindowInsets.Companion.statusBars: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().statusBars

/**
 * For the [WindowInsetsCompat.Type.systemBars].
 */
val WindowInsets.Companion.systemBars: WindowInsets
    @Tunable
    get() {
        val systemBars = WindowInsetsHolder.current().systemBars
        return systemBars
    }

/**
 * For the [WindowInsetsCompat.Type.systemGestures].
 */
val WindowInsets.Companion.systemGestures: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().systemGestures

/**
 * For the [WindowInsetsCompat.Type.tappableElement].
 */
val WindowInsets.Companion.tappableElement: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().tappableElement

/**
 * The insets for the curved areas in a waterfall display.
 */
val WindowInsets.Companion.waterfall: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().waterfall

/**
 * The insets that include areas where content may be covered by other drawn content.
 * This includes all [system bars][systemBars], [display cutout][displayCutout], and
 * [soft keyboard][ime].
 */
val WindowInsets.Companion.safeDrawing: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().safeDrawing

/**
 * The insets that include areas where gestures may be confused with other input,
 * including [system gestures][systemGestures],
 * [mandatory system gestures][mandatorySystemGestures],
 * [rounded display areas][waterfall], and [tappable areas][tappableElement].
 */
val WindowInsets.Companion.safeGestures: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().safeGestures

/**
 * The insets that include all areas that may be drawn over or have gesture confusion,
 * including everything in [safeDrawing] and [safeGestures].
 */
val WindowInsets.Companion.safeContent: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().safeContent

/**
 * The insets that the [WindowInsetsCompat.Type.captionBar] will consume if shown.
 * If it cannot be shown then this will be empty.
 */
val WindowInsets.Companion.captionBarIgnoringVisibility: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().captionBarIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.navigationBars] will consume if shown.
 * These insets represent where system UI places navigation bars. Interactive UI should
 * avoid the navigation bars area. If navigation bars cannot be shown, then this will be
 * empty.
 */
val WindowInsets.Companion.navigationBarsIgnoringVisibility: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().navigationBarsIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.statusBars] will consume if shown.
 * If the status bar can never be shown, then this will be empty.
 */
val WindowInsets.Companion.statusBarsIgnoringVisibility: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().statusBarsIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.systemBars] will consume if shown.
 *
 * If system bars can never be shown, then this will be empty.
 */
val WindowInsets.Companion.systemBarsIgnoringVisibility: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().systemBarsIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.tappableElement] will consume if active.
 *
 * If there are never tappable elements then this is empty.
 */
val WindowInsets.Companion.tappableElementIgnoringVisibility: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().tappableElementIgnoringVisibility

/**
 * `true` when the [caption bar][captionBar] is being displayed, irrespective of
 * whether it intersects with the Window.
 */
val WindowInsets.Companion.isCaptionBarVisible: Boolean
    @Tunable
    get() = WindowInsetsHolder.current().captionBar.isVisible

/**
 * `true` when the [soft keyboard][ime] is being displayed, irrespective of
 * whether it intersects with the Window.
 */
val WindowInsets.Companion.isImeVisible: Boolean
    @Tunable
    get() = WindowInsetsHolder.current().ime.isVisible

/**
 * `true` when the [statusBars] are being displayed, irrespective of
 * whether they intersects with the Window.
 */
val WindowInsets.Companion.areStatusBarsVisible: Boolean
    @Tunable
    get() = WindowInsetsHolder.current().statusBars.isVisible

/**
 * `true` when the [navigationBars] are being displayed, irrespective of
 * whether they intersects with the Window.
 */
val WindowInsets.Companion.areNavigationBarsVisible: Boolean
    @Tunable
    get() = WindowInsetsHolder.current().navigationBars.isVisible

/**
 * `true` when the [systemBars] are being displayed, irrespective of
 * whether they intersects with the Window.
 */
val WindowInsets.Companion.areSystemBarsVisible: Boolean
    @Tunable
    get() = WindowInsetsHolder.current().systemBars.isVisible
/**
 * `true` when the [tappableElement] is being displayed, irrespective of
 * whether they intersects with the Window.
 */
val WindowInsets.Companion.isTappableElementVisible: Boolean
    @Tunable
    get() = WindowInsetsHolder.current().tappableElement.isVisible

/**
 * The [WindowInsets] for the IME before the IME started animating in. The current
 * animated value is [WindowInsets.Companion.ime].
 *
 * This will be the same as [imeAnimationTarget] when there is no IME animation
 * in progress.
 */
val WindowInsets.Companion.imeAnimationSource: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().imeAnimationSource

/**
 * The [WindowInsets] for the IME when the animation completes, if it is allowed
 * to complete successfully. The current animated value is [WindowInsets.Companion.ime].
 *
 * This will be the same as [imeAnimationSource] when there is no IME animation
 * in progress.
 */
val WindowInsets.Companion.imeAnimationTarget: WindowInsets
    @Tunable
    get() = WindowInsetsHolder.current().imeAnimationTarget

/**
 * The insets for various values in the current window.
 */
internal class WindowInsetsHolder private constructor(insets: WindowInsetsCompat?, view: View) {
    val captionBar =
        systemInsets(insets, WindowInsetsCompat.Type.captionBar(), "captionBar")
    val displayCutout =
        systemInsets(insets, WindowInsetsCompat.Type.displayCutout(), "displayCutout")
    val ime = systemInsets(insets, WindowInsetsCompat.Type.ime(), "ime")
    val mandatorySystemGestures = systemInsets(
        insets,
        WindowInsetsCompat.Type.mandatorySystemGestures(),
        "mandatorySystemGestures"
    )
    val navigationBars =
        systemInsets(insets, WindowInsetsCompat.Type.navigationBars(), "navigationBars")
    val statusBars =
        systemInsets(insets, WindowInsetsCompat.Type.statusBars(), "statusBars")
    val systemBars =
        systemInsets(insets, WindowInsetsCompat.Type.systemBars(), "systemBars")
    val systemGestures =
        systemInsets(insets, WindowInsetsCompat.Type.systemGestures(), "systemGestures")
    val tappableElement =
        systemInsets(insets, WindowInsetsCompat.Type.tappableElement(), "tappableElement")
    val waterfall =
        ValueInsets(insets?.displayCutout?.waterfallInsets ?: AndroidXInsets.NONE, "waterfall")
    val safeDrawing =
        systemBars.union(ime).union(displayCutout)
    val safeGestures: WindowInsets =
        tappableElement.union(mandatorySystemGestures).union(systemGestures).union(waterfall)
    val safeContent: WindowInsets = safeDrawing.union(safeGestures)

    val captionBarIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.captionBar(),
        "captionBarIgnoringVisibility"
    )
    val navigationBarsIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets, WindowInsetsCompat.Type.navigationBars(), "navigationBarsIgnoringVisibility"
    )
    val statusBarsIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.statusBars(),
        "statusBarsIgnoringVisibility"
    )
    val systemBarsIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.systemBars(),
        "systemBarsIgnoringVisibility"
    )
    val tappableElementIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.tappableElement(),
        "tappableElementIgnoringVisibility"
    )
    val imeAnimationTarget = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.ime(),
        "imeAnimationTarget"
    )
    val imeAnimationSource = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.ime(),
        "imeAnimationSource"
    )

    /**
     * `true` unless the `AbstractComposeView` [AbstractComposeView.consumeWindowInsets] is set to
     * `false`.
     */
    val consumes = (view.parent as? View)?.getTag(R.id.consume_window_insets_tag)
            as? Boolean ?: true

    /**
     * The number of accesses to [WindowInsetsHolder]. When this reaches
     * zero, the listeners are removed. When it increases to 1, the listeners are added.
     */
    private var accessCount = 0

    private val insetsListener = InsetsListener(this)

    /**
     * A usage of [WindowInsetsHolder.current] was added. We must track so that when the
     * first one is added, listeners are set and when the last is removed, the listeners
     * are removed.
     */
    fun incrementAccessors(view: View) {
        if (accessCount == 0) {
            // add listeners
            ViewCompat.setOnApplyWindowInsetsListener(view, insetsListener)

            if (ViewCompat.isAttachedToWindow(view)) {
                ViewCompat.requestApplyInsets(view)
            }
            view.addOnAttachStateChangeListener(insetsListener)

            ViewCompat.setWindowInsetsAnimationCallback(view, insetsListener)
        }
        accessCount++
    }

    /**
     * A usage of [WindowInsetsHolder.current] was removed. We must track so that when the
     * first one is added, listeners are set and when the last is removed, the listeners
     * are removed.
     */
    fun decrementAccessors(view: View) {
        accessCount--
        if (accessCount == 0) {
            // remove listeners
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
            ViewCompat.setWindowInsetsAnimationCallback(view, null)
            view.removeOnAttachStateChangeListener(insetsListener)
        }
    }

    /**
     * Updates the WindowInsets values and notifies changes.
     */
    fun update(windowInsets: WindowInsetsCompat, types: Int = 0) {
        val insets = windowInsets
        captionBar.update(insets, types)
        ime.update(insets, types)
        displayCutout.update(insets, types)
        navigationBars.update(insets, types)
        statusBars.update(insets, types)
        systemBars.update(insets, types)
        systemGestures.update(insets, types)
        tappableElement.update(insets, types)
        mandatorySystemGestures.update(insets, types)

        if (types == 0) {
            captionBarIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.captionBar()
            ).toInsetsValues()
            navigationBarsIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.navigationBars()
            ).toInsetsValues()
            statusBarsIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.statusBars()
            ).toInsetsValues()
            systemBarsIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars()
            ).toInsetsValues()
            tappableElementIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.tappableElement()
            ).toInsetsValues()

            val cutout = insets.displayCutout
            if (cutout != null) {
                val waterfallInsets = cutout.waterfallInsets
                waterfall.value = waterfallInsets.toInsetsValues()
            }
        }
        Snapshot.sendApplyNotifications()
    }

    /**
     * Updates [WindowInsets.Companion.imeAnimationSource]. It should be called prior to
     * [update].
     */
    fun updateImeAnimationSource(windowInsets: WindowInsetsCompat) {
        imeAnimationSource.value =
            windowInsets.getInsets(WindowInsetsCompat.Type.ime()).toInsetsValues()
    }

    /**
     * Updates [WindowInsets.Companion.imeAnimationTarget]. It should be called prior to
     * [update].
     */
    fun updateImeAnimationTarget(windowInsets: WindowInsetsCompat) {
        imeAnimationTarget.value =
            windowInsets.getInsets(WindowInsetsCompat.Type.ime()).toInsetsValues()
    }

    companion object {
        /**
         * A mapping of AndroidComposeView to ComposeWindowInsets. Normally a tag is a great
         * way to do this mapping, but off-UI thread and multithreaded composition don't
         * allow using the tag.
         */
        private val viewMap = WeakHashMap<View, WindowInsetsHolder>()

        private var testInsets = false

        /**
         * Testing Window Insets is difficult, so we have this to help eliminate device-specifics
         * from the WindowInsets. This is indirect because `@TestOnly` cannot be applied to a
         * property with a backing field.
         */
        @TestOnly
        fun setUseTestInsets(testInsets: Boolean) {
            this.testInsets = testInsets
        }

        @Tunable
        fun current(): WindowInsetsHolder {
            val view = currentView
            val insets = getOrCreateFor(view)

            DisposableEffect(insets) {
                insets.incrementAccessors(view);
                {
                    insets.decrementAccessors(view)
                }
            }
            return insets
        }

        /**
         * Returns the [WindowInsetsHolder] associated with [view] or creates one and associates
         * it.
         */
        private fun getOrCreateFor(view: View): WindowInsetsHolder {
            return synchronized(viewMap) {
                viewMap.getOrPut(view) {
                    val insets = null
                    WindowInsetsHolder(insets, view)
                }
            }
        }

        /**
         * Creates a [ValueInsets] using the value from [windowInsets] if it isn't `null`
         */
        private fun systemInsets(
            windowInsets: WindowInsetsCompat?,
            type: Int,
            name: String
        ) = AndroidWindowInsets(type, name).apply { windowInsets?.let { update(it, type) } }

        /**
         * Creates a [ValueInsets] using the "ignoring visibility" value from [windowInsets]
         * if it isn't `null`
         */
        private fun valueInsetsIgnoringVisibility(
            windowInsets: WindowInsetsCompat?,
            type: Int,
            name: String
        ): ValueInsets {
            val initial = windowInsets?.getInsetsIgnoringVisibility(type) ?: AndroidXInsets.NONE
            return ValueInsets(initial, name)
        }
    }
}

private class InsetsListener(
    val composeInsets: WindowInsetsHolder,
) : WindowInsetsAnimationCompat.Callback(
    if (composeInsets.consumes) DISPATCH_MODE_STOP else DISPATCH_MODE_CONTINUE_ON_SUBTREE
), Runnable, OnApplyWindowInsetsListener, OnAttachStateChangeListener {
    /**
     * When [android.view.WindowInsetsController.controlWindowInsetsAnimation] is called,
     * the [onApplyWindowInsets] is called after [onPrepare] with the target size. We
     * don't want to report the target size, we want to always report the current size,
     * so we must ignore those calls. However, the animation may be canceled before it
     * progresses. On R, it won't make any callbacks, so we have to figure out whether
     * the [onApplyWindowInsets] is from a canceled animation or if it is from the
     * controlled animation. When [prepared] is `true` on R, we post a callback to
     * set the [onApplyWindowInsets] insets value.
     */
    var prepared = false

    /**
     * `true` if there is an animation in progress.
     */
    var runningAnimation = false

    var savedInsets: WindowInsetsCompat? = null

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        prepared = true
        runningAnimation = true
        super.onPrepare(animation)
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat {
        prepared = false
        return super.onStart(animation, bounds)
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        composeInsets.update(insets)
        return if (composeInsets.consumes) WindowInsetsCompat.CONSUMED else insets
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        prepared = false
        runningAnimation = false
        val insets = savedInsets
        if (animation.durationMillis != 0L && insets != null) {
            composeInsets.updateImeAnimationSource(insets)
            composeInsets.updateImeAnimationTarget(insets)
            composeInsets.update(insets)
        }
        savedInsets = null
        super.onEnd(animation)
    }

    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        // Keep track of the most recent insets we've seen, to ensure onEnd will always use the
        // most recently acquired insets
        savedInsets = insets
        composeInsets.updateImeAnimationTarget(insets)
        if (prepared) {
            // There may be no callback on R if the animation is canceled after onPrepare(),
            // so we won't know if the onPrepare() was canceled or if this is an
            // onApplyWindowInsets() after the cancelation. We'll just post the value
            // and if it is still preparing then we just use the value.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                view.post(this)
            }
        } else if (!runningAnimation) {
            // If an animation is running, rely on onProgress() to update the insets
            // On APIs less than 30 where the IME animation is backported, this avoids reporting
            // the final insets for a frame while the animation is running.
            composeInsets.updateImeAnimationSource(insets)
            composeInsets.update(insets)
        }
        return if (composeInsets.consumes) WindowInsetsCompat.CONSUMED else insets
    }

    /**
     * On [R], we don't receive the [onEnd] call when an animation is canceled, so we post
     * the value received in [onApplyWindowInsets] immediately after [onPrepare]. If [onProgress]
     * or [onEnd] is received before the runnable executes then the value won't be used. Otherwise,
     * the [onApplyWindowInsets] value will be used. It may have a janky frame, but it is the best
     * we can do.
     */
    override fun run() {
        if (prepared) {
            prepared = false
            runningAnimation = false
            savedInsets?.let {
                composeInsets.updateImeAnimationSource(it)
                composeInsets.update(it)
                savedInsets = null
            }
        }
    }

    override fun onViewAttachedToWindow(view: View) {
        ViewCompat.requestApplyInsets(view)
    }

    override fun onViewDetachedFromWindow(v: View) {
    }
}