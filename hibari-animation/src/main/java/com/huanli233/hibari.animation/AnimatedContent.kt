package com.huanli233.hibari.animation

import android.view.ViewGroup
import androidx.collection.mutableScatterMapOf
import com.huanli233.hibari.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import com.huanli233.hibari.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import com.huanli233.hibari.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import com.huanli233.hibari.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import com.huanli233.hibari.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Start
import com.huanli233.hibari.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import com.huanli233.hibari.foundation.Box
import com.huanli233.hibari.foundation.attributes.clipToPadding
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.effects.DisposableEffect
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.key
import com.huanli233.hibari.runtime.locals.LocalLayoutDirection
import com.huanli233.hibari.runtime.mutableStateListOf
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.runtime.rememberUpdatedState
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.runtime.util.fastForEach
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.layout.Alignment
import com.huanli233.hibari.ui.layout.ParentDataModifier
import com.huanli233.hibari.ui.node.Node
import com.huanli233.hibari.ui.thenLayoutAttribute
import com.huanli233.hibari.ui.thenUnitLayoutAttribute
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.unit.Density
import com.huanli233.hibari.ui.unit.IntOffset
import com.huanli233.hibari.ui.unit.IntSize
import com.huanli233.hibari.ui.unit.LayoutDirection

class ContentTransform(
    val targetContentEnter: EnterTransition,
    val initialContentExit: ExitTransition,
    targetContentZIndex: Float = 0f,
    sizeTransform: SizeTransform? = SizeTransform(),
) {
    var targetContentZIndex: Float by mutableStateOf(targetContentZIndex)

    var sizeTransform: SizeTransform? = sizeTransform
        internal set
}

fun SizeTransform(
    clip: Boolean = true,
    sizeAnimationSpec: (initialSize: IntSize, targetSize: IntSize) -> FiniteAnimationSpec<IntSize> =
        { _, _ ->
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold,
            )
        },
): SizeTransform = SizeTransformImpl(clip, sizeAnimationSpec)

interface SizeTransform {
    val clip: Boolean

    fun createAnimationSpec(
        initialSize: IntSize,
        targetSize: IntSize,
    ): FiniteAnimationSpec<IntSize>
}

private class SizeTransformImpl(
    override val clip: Boolean = true,
    val sizeAnimationSpec:
        (initialSize: IntSize, targetSize: IntSize) -> FiniteAnimationSpec<IntSize>,
) : SizeTransform {
    override fun createAnimationSpec(
        initialSize: IntSize,
        targetSize: IntSize,
    ): FiniteAnimationSpec<IntSize> = sizeAnimationSpec(initialSize, targetSize)
}

infix fun EnterTransition.togetherWith(exit: ExitTransition): ContentTransform =
    ContentTransform(this, exit)

sealed interface AnimatedContentTransitionScope<S> : Transition.Segment<S> {
    infix fun ContentTransform.using(sizeTransform: SizeTransform?): ContentTransform

    @JvmInline
    value class SlideDirection internal constructor(private val value: Int) {
        companion object {
            val Left: SlideDirection = SlideDirection(0)
            val Right: SlideDirection = SlideDirection(1)
            val Up: SlideDirection = SlideDirection(2)
            val Down: SlideDirection = SlideDirection(3)
            val Start: SlideDirection = SlideDirection(4)
            val End: SlideDirection = SlideDirection(5)
        }

        override fun toString(): String {
            return when (this) {
                Left -> "Left"
                Right -> "Right"
                Up -> "Up"
                Down -> "Down"
                Start -> "Start"
                End -> "End"
                else -> "Invalid"
            }
        }
    }

    fun slideIntoContainer(
        towards: SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset> =
            spring(visibilityThreshold = IntOffset.VisibilityThreshold),
        initialOffset: (offsetForFullSlide: Int) -> Int = { it },
    ): EnterTransition

    fun slideOutOfContainer(
        towards: SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset> =
            spring(visibilityThreshold = IntOffset.VisibilityThreshold),
        targetOffset: (offsetForFullSlide: Int) -> Int = { it },
    ): ExitTransition

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    val ExitTransition.Companion.KeepUntilTransitionsFinished: ExitTransition
        get() = KeepUntilTransitionsFinished

    /** This returns the [Alignment] specified on [AnimatedContent]. */
    val contentAlignment: Alignment
}

internal class AnimatedContentTransitionScopeImpl<S>
internal constructor(
    internal val transition: Transition<S>,
    override var contentAlignment: Alignment,
    internal var layoutDirection: LayoutDirection,
) : AnimatedContentTransitionScope<S> {
    /** Initial state of a Transition Segment. This is the state that transition starts from. */
    override val initialState: S
        @Suppress("UnknownNullness") get() = transition.segment.initialState

    /** Target state of a Transition Segment. This is the state that transition will end on. */
    override val targetState: S
        @Suppress("UnknownNullness") get() = transition.segment.targetState

    override infix fun ContentTransform.using(sizeTransform: SizeTransform?) =
        this.apply { this.sizeTransform = sizeTransform }

    override fun slideIntoContainer(
        towards: AnimatedContentTransitionScope.SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset>,
        initialOffset: (offsetForFullSlide: Int) -> Int,
    ): EnterTransition =
        when {
            towards.isLeft ->
                slideInHorizontally(animationSpec) {
                    initialOffset.invoke(
                        currentSize.width - calculateOffset(IntSize(it, it), currentSize).x
                    )
                }
            towards.isRight ->
                slideInHorizontally(animationSpec) {
                    initialOffset.invoke(-calculateOffset(IntSize(it, it), currentSize).x - it)
                }
            towards == Up ->
                slideInVertically(animationSpec) {
                    initialOffset.invoke(
                        currentSize.height - calculateOffset(IntSize(it, it), currentSize).y
                    )
                }
            towards == Down ->
                slideInVertically(animationSpec) {
                    initialOffset.invoke(-calculateOffset(IntSize(it, it), currentSize).y - it)
                }
            else -> EnterTransition.None
        }

    private val AnimatedContentTransitionScope.SlideDirection.isLeft: Boolean
        get() {
            return this == Left ||
                    this == Start && layoutDirection == LayoutDirection.Ltr ||
                    this == End && layoutDirection == LayoutDirection.Rtl
        }

    private val AnimatedContentTransitionScope.SlideDirection.isRight: Boolean
        get() {
            return this == Right ||
                    this == Start && layoutDirection == LayoutDirection.Rtl ||
                    this == End && layoutDirection == LayoutDirection.Ltr
        }

    private fun calculateOffset(fullSize: IntSize, currentSize: IntSize): IntOffset {
        return contentAlignment.align(fullSize, currentSize, LayoutDirection.Ltr)
    }

    override fun slideOutOfContainer(
        towards: AnimatedContentTransitionScope.SlideDirection,
        animationSpec: FiniteAnimationSpec<IntOffset>,
        targetOffset: (offsetForFullSlide: Int) -> Int,
    ): ExitTransition {
        return when {
            // Note: targetSize could be 0 for empty composables
            towards.isLeft ->
                slideOutHorizontally(animationSpec) {
                    val targetSize = targetSizeMap[transition.targetState]?.value ?: IntSize.Zero
                    targetOffset.invoke(-calculateOffset(IntSize(it, it), targetSize).x - it)
                }
            towards.isRight ->
                slideOutHorizontally(animationSpec) {
                    val targetSize = targetSizeMap[transition.targetState]?.value ?: IntSize.Zero
                    targetOffset.invoke(
                        -calculateOffset(IntSize(it, it), targetSize).x + targetSize.width
                    )
                }
            towards == Up ->
                slideOutVertically(animationSpec) {
                    val targetSize = targetSizeMap[transition.targetState]?.value ?: IntSize.Zero
                    targetOffset.invoke(-calculateOffset(IntSize(it, it), targetSize).y - it)
                }
            towards == Down ->
                slideOutVertically(animationSpec) {
                    val targetSize = targetSizeMap[transition.targetState]?.value ?: IntSize.Zero
                    targetOffset.invoke(
                        -calculateOffset(IntSize(it, it), targetSize).y + targetSize.height
                    )
                }
            else -> ExitTransition.None
        }
    }

    internal var measuredSize: IntSize by mutableStateOf(IntSize.Zero)
    internal val targetSizeMap = mutableScatterMapOf<S, com.huanli233.hibari.runtime.State<IntSize>>()
    internal var animatedSize: com.huanli233.hibari.runtime.State<IntSize>? = null

    // Current size of the container. If there's any size animation, the current size will be
    // read from the animation value, otherwise we'll use the current
    private val currentSize: IntSize
        get() = animatedSize?.value ?: measuredSize

    @Suppress("ComposableModifierFactory", "ModifierFactoryExtensionFunction")
    @Tunable
    internal fun createSizeAnimationModifier(contentTransform: ContentTransform): Modifier {
        var shouldAnimateSize by remember(this) { mutableStateOf(false) }
        val sizeTransform = rememberUpdatedState(contentTransform.sizeTransform)
        if (transition.currentState == transition.targetState) {
            shouldAnimateSize = false
        } else {
            // TODO: CurrentSize is only relevant to enter/exit transition, not so much for sizeAnim
            if (sizeTransform.value != null) {
                shouldAnimateSize = true
            }
        }
        val sizeAnimation: Transition<S>.DeferredAnimation<IntSize, AnimationVector2D>?
        return if (shouldAnimateSize) {
            val sizeAnimation = transition.createDeferredAnimation(IntSize.VectorConverter)
            // *** START: COMPLETED SECTION ***
            remember(sizeAnimation) {
                val clipModifier =
                    if (sizeTransform.value?.clip == true) Modifier.clipToPadding(false) else Modifier

                val animatedSizeState = sizeAnimation.animate(
                    transitionSpec = {
                        val spec = sizeTransform.value!!
                        val initial = targetSizeMap[initialState]?.value ?: measuredSize
                        val target = targetSizeMap[targetState]?.value ?: measuredSize
                        spec.createAnimationSpec(initial, target)
                    }
                ) { state ->
                    targetSizeMap[state]?.value ?: measuredSize
                }

                animatedSize = animatedSizeState

                val sizeModifier = Modifier.thenLayoutAttribute<ViewGroup.LayoutParams, com.huanli233.hibari.runtime.State<IntSize>>(
                    uniqueKey,
                    animatedSizeState
                ) {  state, _ ->
                    val size = state.value
                    if (size.width != Int.MIN_VALUE && size.height != Int.MIN_VALUE) {
                        width = size.width
                        height = size.height
                    }
                }
                clipModifier.then(sizeModifier)
            }
            // *** END: COMPLETED SECTION ***
        } else {
            animatedSize = null
            Modifier
        }
    }

    // This helps track the target measurable without affecting the placement order. Target
    // measurable needs to be measured first but placed last.
    internal class ChildData(isTarget: Boolean) : ParentDataModifier {
        // isTarget is read during measure. It is necessary to make this a MutableState
        // such that when the target changes, measure is triggered
        var isTarget by mutableStateOf(isTarget)

        override fun modifyParentData(parentData: Any?): Any {
            return this@ChildData
        }
    }
}

private val UnspecifiedSize: IntSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)

/**
 * Receiver scope for content lambda for AnimatedContent. In this scope,
 * [transition][AnimatedVisibilityScope.transition] can be used to observe the state of the
 * transition, or to add more enter/exit transition for the content.
 */
sealed interface AnimatedContentScope : AnimatedVisibilityScope

private class AnimatedContentScopeImpl
internal constructor(animatedVisibilityScope: AnimatedVisibilityScope) :
    AnimatedContentScope, AnimatedVisibilityScope by animatedVisibilityScope

@OptIn(ExperimentalAnimationApi::class)
@Tunable
fun <S> Transition<S>.AnimatedContent(
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
            .togetherWith(fadeOut(animationSpec = tween(90)))
    },
    contentAlignment: Alignment = Alignment.TopStart,
    contentKey: (targetState: S) -> Any? = { it },
    content: @Tunable AnimatedContentScope.(targetState: S) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val rootScope =
        remember(this) {
            AnimatedContentTransitionScopeImpl(this, contentAlignment, layoutDirection)
        }
    // TODO: remove screen as soon as they are animated out
    val currentlyVisible = remember(this) { mutableStateListOf(currentState) }
    val contentMap = remember(this) { mutableScatterMapOf<S, @Tunable () -> Unit>() }
    // This is needed for tooling because it could change currentState directly,
    // as opposed to changing target only. When that happens we need to clear all the
    // visible content and only display the content for the new current state and target state.
    if (!currentlyVisible.contains(currentState)) {
        currentlyVisible.clear()
        currentlyVisible.add(currentState)
    }
    if (currentState == targetState) {
        if (currentlyVisible.size != 1 || currentlyVisible[0] != currentState) {
            currentlyVisible.clear()
            currentlyVisible.add(currentState)
        }
        if (contentMap.size != 1 || contentMap.containsKey(currentState)) {
            contentMap.clear()
        }
        // TODO: Do we want to support changing contentAlignment amid animation?
        rootScope.contentAlignment = contentAlignment
        rootScope.layoutDirection = layoutDirection
    }
    // Currently visible list always keeps the targetState at the end of the list, unless it's
    // already in the list in the case of interruption. This makes the composable associated with
    // the targetState get placed last, so the target composable will be displayed on top of
    // content associated with other states, unless zIndex is specified.
    if (currentState != targetState && !currentlyVisible.contains(targetState)) {
        // Replace the target with the same key if any
        val id = currentlyVisible.indexOfFirst { contentKey(it) == contentKey(targetState) }
        if (id == -1) {
            currentlyVisible.add(targetState)
        } else {
            currentlyVisible[id] = targetState
        }
    }
    if (!contentMap.containsKey(targetState) || !contentMap.containsKey(currentState)) {
        contentMap.clear()
        currentlyVisible.forEach { stateForContent ->
            contentMap[stateForContent] = @Tunable {
                val specOnEnter = remember { transitionSpec(rootScope) }
                // NOTE: enter and exit for this AnimatedVisibility will be using different spec,
                // naturally.
                val exit =
                    remember(segment.targetState == stateForContent) {
                        if (segment.targetState == stateForContent) {
                            ExitTransition.None
                        } else {
                            rootScope.transitionSpec().initialContentExit
                        }
                    }
                val childData = remember {
                    AnimatedContentTransitionScopeImpl.ChildData(stateForContent == targetState)
                }
                // TODO: Will need a custom impl of this to: 1) get the signal for when
                // the animation is finished, 2) get the target size properly
                AnimatedEnterExitImpl(
                    this,
                    { it == stateForContent },
                    enter = specOnEnter.targetContentEnter,
                    exit = exit,
                    modifier = Modifier
                            .then(childData.apply { isTarget = stateForContent == targetState }),
                    shouldDisposeBlock = { currentState, targetState ->
                        currentState == EnterExitState.PostExit &&
                                targetState == EnterExitState.PostExit &&
                                !exit.data.hold
                    },
                ) {
                    // TODO: Should Transition.AnimatedVisibility have an end listener?
                    DisposableEffect(this) {
                        {
                            currentlyVisible.remove(stateForContent)
                            rootScope.targetSizeMap.remove(stateForContent)
                        }
                    }
                    rootScope.targetSizeMap[stateForContent] =
                        (this as AnimatedVisibilityScopeImpl).targetSize
                    with(remember { AnimatedContentScopeImpl(this) }) { content(stateForContent) }
                }
            }
        }
    }
    val contentTransform = remember(rootScope, segment) { transitionSpec(rootScope) }
    val sizeModifier = rootScope.createSizeAnimationModifier(contentTransform)
    Box {
        currentlyVisible.fastForEach { key(contentKey(it)) { contentMap[it]?.invoke() } }
    }
}