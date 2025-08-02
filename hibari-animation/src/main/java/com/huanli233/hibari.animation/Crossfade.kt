package com.huanli233.hibari.animation

import androidx.collection.mutableScatterMapOf
import com.huanli233.hibari.foundation.Box
import com.huanli233.hibari.foundation.attributes.alpha
import com.huanli233.hibari.foundation.attributes.matchParentSize
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.key
import com.huanli233.hibari.runtime.mutableStateListOf
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.ui.Modifier

@Tunable
fun <T> Crossfade(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    label: String = "Crossfade",
    content: @Tunable (T) -> Unit,
) {
    val transition = updateTransition(targetState, label)
    transition.Crossfade(modifier, animationSpec, content = content)
}

/**
 * [Crossfade] allows to switch between two layouts with a crossfade animation. The target state of
 * this Crossfade will be the target state of the given Transition object. In other words, when the
 * Transition changes target, the [Crossfade] will fade in the target content while fading out the
 * current content.
 *
 * [content] is a mapping between the state and the composable function for the content of that
 * state. During the crossfade, [content] lambda will be invoked multiple times with different state
 * parameter such that content associated with different states will be fading in/out at the same
 * time.
 *
 * [contentKey] will be used to perform equality check for different states. For example, when two
 * states resolve to the same content key, there will be no animation for that state change. By
 * default, [contentKey] is the same as the state object. [contentKey] can be particularly useful if
 * target state object gets recreated across save & restore while a more persistent key is needed to
 * properly restore the internal states of the content.
 *
 * @param modifier Modifier to be applied to the animation container.
 * @param animationSpec the [AnimationSpec] to configure the animation.
 * @param contentKey A mapping from a given state to an object of [Any].
 * @param content A mapping from a given state to the content corresponding to that state.
 */
@Tunable
fun <T> Transition<T>.Crossfade(
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    contentKey: (targetState: T) -> Any? = { it },
    content: @Tunable (targetState: T) -> Unit,
) {
    val currentlyVisible = remember { mutableStateListOf<T>().apply { add(currentState) } }
    val contentMap = remember { mutableScatterMapOf<T, @Tunable () -> Unit>() }
    if (currentState == targetState) {
        // If not animating, just display the current state
        if (currentlyVisible.size != 1 || currentlyVisible[0] != targetState) {
            // Remove all the intermediate items from the list once the animation is finished.
            currentlyVisible.removeAll { it != targetState }
            contentMap.clear()
        }
    }
    if (targetState !in contentMap) {
        // Replace target with the same key if any
        val replacementId =
            currentlyVisible.indexOfFirst { contentKey(it) == contentKey(targetState) }
        if (replacementId == -1) {
            currentlyVisible.add(targetState)
        } else {
            currentlyVisible[replacementId] = targetState
        }
        contentMap.clear()
        currentlyVisible.forEach { stateForContent ->
            contentMap[stateForContent] = @Tunable {
                val alpha by animateFloat(transitionSpec = { animationSpec }) {
                    if (it == stateForContent) 1f else 0f
                }
                Box(Modifier.alpha(alpha).matchParentSize()) { content(stateForContent) }
            }
        }
    }

    Box(modifier) {
        currentlyVisible.forEach { key(contentKey(it)) { contentMap[it]?.invoke() } }
    }
}