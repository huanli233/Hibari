package com.huanli233.hibari.animation

/** Possible reasons for [Animatable]s to end. */
enum class AnimationEndReason {
    /**
     * Animation will be forced to end when its value reaches upper/lower bound (if they have been
     * defined, e.g. via [Animatable.updateBounds])
     *
     * Unlike [Finished], when an animation ends due to [BoundReached], it often falls short from
     * its initial target, and the remaining velocity is often non-zero. Both the end value and the
     * remaining velocity can be obtained via [AnimationResult].
     */
    BoundReached,
    /** Animation has finished successfully without any interruption. */
    Finished,
}