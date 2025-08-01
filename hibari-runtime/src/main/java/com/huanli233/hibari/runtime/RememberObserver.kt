package com.huanli233.hibari.runtime

/**
 * An interface for objects stored by `remember` to observe their lifecycle within the composition.
 */
interface RememberObserver {
    /**
     * Called when the observer is first remembered and successfully stored in the composition.
     */
    fun onRemembered()

    /**
     * Called when the observer is about to be removed from the composition.
     * This can happen if the `remember` call is no longer part of the composition,
     * or if its keys have changed, causing a new instance to be created.
     */
    fun onForgotten()

    /**
     * Called when the composition that remembers this observer is discarded before it can be committed.
     * For simplicity, we can delegate this to `onForgotten`.
     */
    fun onAbandoned() = onForgotten()
}