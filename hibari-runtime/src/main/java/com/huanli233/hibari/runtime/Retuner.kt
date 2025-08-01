package com.huanli233.hibari.runtime

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import com.huanli233.hibari.ui.HibariFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

class Retuner(
    private val coroutineContext: CoroutineContext,
    private val factories: List<HibariFactory>
) {

    private val scope = CoroutineScope(coroutineContext + SupervisorJob())
    private val invalidations = ConcurrentLinkedQueue<Tunation>()
    @Volatile
    private var currentTuneJob: Job? = null
    private var isRunning = false

    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())
    }

    fun scheduleRetune(session: Tunation) {
        if (invalidations.add(session)) {
            startRetuneLoop()
        }
    }

    fun tuneNow(session: Tunation) {
        TuneController.tune(session, factories)
    }

    private fun startRetuneLoop() {
        if (!isRunning) {
            isRunning = true
            scope.launch {
                runRetuneLoop()
            }
        }
    }

    private suspend fun runRetuneLoop() {
        while (isRunning) {
            awaitFrame()

            if (invalidations.isNotEmpty()) {
                currentTuneJob?.cancel("A new Tune was scheduled")

                currentTuneJob = scope.launch {
                    val sessionsToTune = drainInvalidations()

                    sessionsToTune.forEach { session ->
                        ensureActive()
                        TuneController.tune(session, factories)
                    }
                }
            }

            if (invalidations.isEmpty()) {
                isRunning = false
            }
        }
    }

    private fun drainInvalidations(): Set<Tunation> {
        val sessions = mutableSetOf<Tunation>()
        while (invalidations.isNotEmpty()) {
            invalidations.poll()?.let { sessions.add(it) }
        }
        return sessions
    }

    private suspend fun awaitFrame() {
        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Choreographer.getInstance().postFrameCallback {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            } else {
                mainHandler.postDelayed({
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }, 16)
            }
        }
    }
}