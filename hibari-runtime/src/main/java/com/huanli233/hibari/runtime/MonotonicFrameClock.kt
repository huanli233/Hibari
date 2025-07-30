package com.huanli233.hibari.runtime

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

interface MonotonicFrameClock : CoroutineContext.Element {
    /**
     * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
     * in nanoseconds in the calling context of frame dispatch, then resumes with the result from
     * [onFrame].
     *
     * `frameTimeNanos` should be used when calculating animation time deltas from frame to frame
     * as it may be normalized to the target time for the frame, not necessarily a direct,
     * "now" value.
     *
     * The time base of the value provided by [withFrameNanos] is implementation defined.
     * Time values provided are strictly monotonically increasing; after a call to [withFrameNanos]
     * completes it must not provide the same value again for a subsequent call.
     */
    suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R

    override val key: CoroutineContext.Key<*> get() = MonotonicFrameClock

    companion object Key : CoroutineContext.Key<MonotonicFrameClock>
}

/**
 * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
 * in milliseconds in the calling context of frame dispatch, then resumes with the result from
 * [onFrame].
 *
 * `frameTimeMillis` should be used when calculating animation time deltas from frame to frame
 * as it may be normalized to the target time for the frame, not necessarily a direct,
 * "now" value.
 *
 * The time base of the value provided by [MonotonicFrameClock.withFrameMillis] is
 * implementation defined. Time values provided are monotonically increasing; after a call to
 * [withFrameMillis] completes it must not provide a smaller value for a subsequent call.
 */
@Suppress("UnnecessaryLambdaCreation")
suspend inline fun <R> MonotonicFrameClock.withFrameMillis(
    crossinline onFrame: (frameTimeMillis: Long) -> R
): R = withFrameNanos { onFrame(it / 1_000_000L) }

/**
 * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
 * in nanoseconds in the calling context of frame dispatch, then resumes with the result from
 * [onFrame].
 *
 * `frameTimeNanos` should be used when calculating animation time deltas from frame to frame
 * as it may be normalized to the target time for the frame, not necessarily a direct,
 * "now" value.
 *
 * The time base of the value provided by [withFrameNanos] is implementation defined.
 * Time values provided are strictly monotonically increasing; after a call to [withFrameNanos]
 * completes it must not provide the same value again for a subsequent call.
 *
 * This function will invoke [MonotonicFrameClock.withFrameNanos] using the calling
 * [CoroutineContext]'s [MonotonicFrameClock] and will throw an [IllegalStateException] if one is
 * not present in the [CoroutineContext].
 */
suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
    coroutineContext.monotonicFrameClock.withFrameNanos(onFrame)

/**
 * Suspends until a new frame is requested, immediately invokes [onFrame] with the frame time
 * in milliseconds in the calling context of frame dispatch, then resumes with the result from
 * [onFrame].
 *
 * `frameTimeMillis` should be used when calculating animation time deltas from frame to frame
 * as it may be normalized to the target time for the frame, not necessarily a direct,
 * "now" value.
 *
 * The time base of the value provided by [MonotonicFrameClock.withFrameMillis] is
 * implementation defined. Time values provided are monotonically increasing; after a call to
 * [withFrameMillis] completes it must not provide a smaller value for a subsequent call.
 *
 * This function will invoke [MonotonicFrameClock.withFrameNanos] using the calling
 * [CoroutineContext]'s [MonotonicFrameClock] and will throw an [IllegalStateException] if one is
 * not present in the [CoroutineContext].
 */
suspend fun <R> withFrameMillis(onFrame: (frameTimeMillis: Long) -> R): R =
    coroutineContext.monotonicFrameClock.withFrameMillis(onFrame)

/**
 * Returns the [MonotonicFrameClock] for this [CoroutineContext] or throws [IllegalStateException]
 * if one is not present.
 */
val CoroutineContext.monotonicFrameClock: MonotonicFrameClock
    get() = this[MonotonicFrameClock] ?: error(
        "A MonotonicFrameClock is not available in this CoroutineContext. Callers should supply " +
                "an appropriate MonotonicFrameClock using withContext."
    )
