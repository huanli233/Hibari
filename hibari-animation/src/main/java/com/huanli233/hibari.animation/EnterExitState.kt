package com.huanli233.hibari.animation

public enum class EnterExitState {
    /** The initial state of a custom enter animation in [AnimatedVisibility].. */
    PreEnter,

    /**
     * The `Visible` state is the target state of a custom *enter* animation, also the initial state
     * of a custom *exit* animation in [AnimatedVisibility].
     */
    Visible,

    /** Target state of a custom *exit* animation in [AnimatedVisibility]. */
    PostExit,
}