package com.huanli233.hibari.animation

import android.view.View
import android.view.ViewGroup
import com.huanli233.hibari.animation.createDeferredAnimation
import com.huanli233.hibari.foundation.Box
import com.huanli233.hibari.foundation.ColumnScope
import com.huanli233.hibari.foundation.RowScope
import com.huanli233.hibari.foundation.attributes.alpha
import com.huanli233.hibari.foundation.attributes.scaleX
import com.huanli233.hibari.foundation.attributes.scaleY
import com.huanli233.hibari.foundation.attributes.translationX
import com.huanli233.hibari.foundation.attributes.translationY
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.currentTuner
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.key
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.produceState
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.runtime.rememberUpdatedState
import com.huanli233.hibari.runtime.snapshotFlow
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.graphics.TransformOrigin
import com.huanli233.hibari.ui.thenLayoutAttribute
import com.huanli233.hibari.ui.thenUnitLayoutAttribute
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.unit.IntOffset
import com.huanli233.hibari.ui.unit.IntSize

@Tunable
public fun ColumnScope.AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = fadeOut() + shrinkVertically(),
    label: String = "AnimatedVisibility",
    content: @Tunable AnimatedVisibilityScope.() -> Unit,
) {
    val transition = updateTransition(visible, label)
    AnimatedVisibilityImpl(transition, { it }, modifier, enter, exit, content = content)
}

@Tunable
fun AnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = fadeOut() + shrinkOut(),
    label: String = "AnimatedVisibility",
    content: @Tunable() AnimatedVisibilityScope.() -> Unit,
) {
    val transition = rememberTransition(visibleState, label)
    AnimatedVisibilityImpl(transition, { it }, modifier, enter, exit, content = content)
}

@Tunable
fun RowScope.AnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = expandHorizontally() + fadeIn(),
    exit: ExitTransition = shrinkHorizontally() + fadeOut(),
    label: String = "AnimatedVisibility",
    content: @Tunable() AnimatedVisibilityScope.() -> Unit,
) {
    val transition = rememberTransition(visibleState, label)
    AnimatedVisibilityImpl(transition, { it }, modifier, enter, exit, content = content)
}

@Tunable
fun ColumnScope.AnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = expandVertically() + fadeIn(),
    exit: ExitTransition = shrinkVertically() + fadeOut(),
    label: String = "AnimatedVisibility",
    content: @Tunable() AnimatedVisibilityScope.() -> Unit,
) {
    val transition = rememberTransition(visibleState, label)
    AnimatedVisibilityImpl(transition, { it }, modifier, enter, exit, content = content)
}

@Tunable
fun <T> Transition<T>.AnimatedVisibility(
    visible: (T) -> Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    content: @Tunable() AnimatedVisibilityScope.() -> Unit,
): Unit = AnimatedVisibilityImpl(this, visible, modifier, enter, exit, content = content)

interface AnimatedVisibilityScope {
    /**
     * [transition] allows custom enter/exit animations to be specified. It will run simultaneously
     * with the built-in enter/exit transitions specified in [AnimatedVisibility].
     */
    val transition: Transition<EnterExitState>

    @Tunable
    fun Modifier.animateEnterExit(
        enter: EnterTransition,
        exit: ExitTransition,
        label: String,
    ): Modifier
}

internal class AnimatedVisibilityScopeImpl
internal constructor(override var transition: Transition<EnterExitState>) : AnimatedVisibilityScope {
    internal val targetSize = mutableStateOf(IntSize.Zero)
    @Tunable
    override fun Modifier.animateEnterExit(
        enter: EnterTransition,
        exit: ExitTransition,
        label: String,
    ): Modifier {
        val E = enter.data
        val X = exit.data

        // 1. Set up deferred animations for all animatable properties
        val tuner = currentTuner
        val alpha = transition.createDeferredAnimation(Float.VectorConverter)
        val scale = transition.createDeferredAnimation(Float.VectorConverter)
        val slide = transition.createDeferredAnimation(IntOffset.VectorConverter)
        val transformOrigin = transition.createDeferredAnimation(TransformOriginVectorConverter)
        val size = transition.createDeferredAnimation(IntSize.VectorConverter)

        // 2. Animate the properties based on the transition state
        val animatedAlpha = alpha.animate(
            transitionSpec = {
                when {
                    EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible -> E.fade?.animationSpec
                    EnterExitState.Visible isTransitioningTo EnterExitState.PostExit -> X.fade?.animationSpec
                    else -> null
                } ?: spring()
            }
        ) { state ->
            when (state) {
                EnterExitState.PreEnter -> E.fade?.alpha ?: 1f
                EnterExitState.Visible -> 1f
                EnterExitState.PostExit -> X.fade?.alpha ?: 1f
            }
        }

        val animatedScale = scale.animate(
            transitionSpec = {
                when {
                    EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible -> E.scale?.animationSpec
                    EnterExitState.Visible isTransitioningTo EnterExitState.PostExit -> X.scale?.animationSpec
                    else -> null
                } ?: spring()
            }
        ) { state ->
            when (state) {
                EnterExitState.PreEnter -> E.scale?.scale ?: 1f
                EnterExitState.Visible -> 1f
                EnterExitState.PostExit -> X.scale?.scale ?: 1f
            }
        }

        val animatedTransformOrigin = transformOrigin.animate(transitionSpec = { spring() }) { state ->
            val enterOrigin = E.scale?.transformOrigin
            val exitOrigin = X.scale?.transformOrigin
            when (state) {
                EnterExitState.PreEnter -> enterOrigin ?: exitOrigin
                EnterExitState.Visible -> if (transition.segment.initialState == EnterExitState.PreEnter) enterOrigin else exitOrigin
                EnterExitState.PostExit -> exitOrigin ?: enterOrigin
            } ?: TransformOrigin.Center
        }

        val animatedSlide = slide.animate(
            transitionSpec = {
                when {
                    EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible -> E.slide?.animationSpec
                    EnterExitState.Visible isTransitioningTo EnterExitState.PostExit -> X.slide?.animationSpec
                    else -> null
                } ?: spring()
            }
        ) { state ->
            when (state) {
                EnterExitState.PreEnter -> E.slide?.slideOffset?.invoke(targetSize.value) ?: IntOffset.Zero
                EnterExitState.Visible -> IntOffset.Zero
                EnterExitState.PostExit -> X.slide?.slideOffset?.invoke(targetSize.value) ?: IntOffset.Zero
            }
        }

        val animatedSize = size.animate(
            transitionSpec = {
                when {
                    EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible -> E.changeSize?.animationSpec
                    EnterExitState.Visible isTransitioningTo EnterExitState.PostExit -> X.changeSize?.animationSpec
                    else -> null
                } ?: spring()
            }
        ) { state ->
            when (state) {
                EnterExitState.PreEnter -> E.changeSize?.size?.invoke(targetSize.value) ?: targetSize.value
                EnterExitState.Visible -> targetSize.value
                EnterExitState.PostExit -> X.changeSize?.size?.invoke(targetSize.value) ?: targetSize.value
            }
        }

        return this
            .thenViewAttribute<View, Float>(uniqueKey, animatedAlpha.value) { alphaValue ->
                if (E.fade != null || X.fade != null) this.alpha = alphaValue
            }
            .thenViewAttribute<View, Pair<Float, TransformOrigin>>(uniqueKey, animatedScale.value to animatedTransformOrigin.value) { (scaleValue, origin) ->
                if (E.scale != null || X.scale != null) {
                    scaleX = scaleValue
                    scaleY = scaleValue
                    pivotX = origin.pivotFractionX * width
                    pivotY = origin.pivotFractionY * height
                }
            }
            .thenViewAttribute<View, IntOffset>(uniqueKey, animatedSlide.value) { offset ->
                if (E.slide != null || X.slide != null) {
                    translationX = offset.x.toFloat()
                    translationY = offset.y.toFloat()
                }
            }
            .thenLayoutAttribute<ViewGroup.LayoutParams, IntSize>(uniqueKey, animatedSize.value) { density, sizeValue ->
                if (E.changeSize != null || X.changeSize != null) {
                    width = sizeValue.width
                    height = sizeValue.height
                }
            }
    }
}

/**
 * RowScope and ColumnScope AnimatedVisibility extensions and AnimatedVisibility without a receiver
 * converge here. AnimatedVisibilityImpl sets up 2 things: 1) It adds a modifier to report 0 size in
 * lookahead when animating out. 2) It sets up a criteria for when content should be disposed.
 */
@Tunable
internal fun <T> AnimatedVisibilityImpl(
    transition: Transition<T>,
    visible: (T) -> Boolean,
    modifier: Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Tunable() AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedEnterExitImpl(
        transition = transition,
        modifier = modifier,
        visible = visible,
        enter = enter,
        exit = exit,
        shouldDisposeBlock = { current, target -> current == target && target == EnterExitState.PostExit },
        content = content,
    )
}

/** Observes lookahead size. */
internal fun interface OnLookaheadMeasured {
    fun invoke(size: IntSize)
}

@Tunable
internal fun <T> AnimatedEnterExitImpl(
    transition: Transition<T>,
    visible: (T) -> Boolean,
    modifier: Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    shouldDisposeBlock: (EnterExitState, EnterExitState) -> Boolean,
    onLookaheadMeasured: OnLookaheadMeasured? = null,
    content: @Tunable() AnimatedVisibilityScope.() -> Unit,
) {
    if (
        visible(transition.targetState) ||
        visible(transition.currentState) ||
        transition.isSeeking ||
        transition.hasInitialValueAnimations
    ) {
        val childTransition =
            transition.createChildTransition(label = "EnterExitTransition") {
                transition.targetEnterExit(visible, it)
            }

        val shouldDisposeBlockUpdated by rememberUpdatedState(shouldDisposeBlock)

        val shouldDisposeAfterExit by
        produceState(
            initialValue =
                shouldDisposeBlock(childTransition.currentState, childTransition.targetState)
        ) {
            snapshotFlow { childTransition.exitFinished }
                .collect {
                    value =
                        if (it) {
                            shouldDisposeBlockUpdated(
                                childTransition.currentState,
                                childTransition.targetState,
                            )
                        } else {
                            false
                        }
                }
        }

        if (!childTransition.exitFinished || !shouldDisposeAfterExit) {
            val scope = remember(transition) { AnimatedVisibilityScopeImpl(childTransition) }
            with(scope) {
                Box(modifier = modifier.animateEnterExit(enter, exit, "AnimateEnterExit")) {
                    content()
                }
            }
        }
    }
}

private val Transition<EnterExitState>.exitFinished
    get() = currentState == EnterExitState.PostExit && targetState == EnterExitState.PostExit

// This converts Boolean visible to EnterExitState
@Tunable
private fun <T> Transition<T>.targetEnterExit(
    visible: (T) -> Boolean,
    targetState: T,
): EnterExitState =
    key(this) {
        if (this.isSeeking) {
            if (visible(targetState)) {
                EnterExitState.Visible
            } else {
                if (visible(this.currentState)) {
                    EnterExitState.PostExit
                } else {
                    EnterExitState.PreEnter
                }
            }
        } else {
            val hasBeenVisible = remember { mutableStateOf(false) }
            if (visible(currentState)) {
                hasBeenVisible.value = true
            }
            if (visible(targetState)) {
                EnterExitState.Visible
            } else {
                // If never been visible, visible = false means PreEnter, otherwise PostExit
                if (hasBeenVisible.value) {
                    EnterExitState.PostExit
                } else {
                    EnterExitState.PreEnter
                }
            }
        }
    }
