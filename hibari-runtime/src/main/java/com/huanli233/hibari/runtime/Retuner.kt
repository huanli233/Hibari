package com.huanli233.hibari.runtime

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.ListUpdateCallback
import com.highcapable.yukireflection.type.android.LayoutInflater_FactoryClass
import com.huanli233.hibari.ui.HibariFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.io.path.Path

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

    fun scheduleTune(session: Tunation) {
        if (invalidations.add(session)) {
            start()
        }
    }

    private fun start() {
        if (!isRunning) {
            isRunning = true
            scope.launch {
                runTuneLoop()
            }
        }
    }

    fun tuneNow(session: Tunation) {
        val tuner = session.tuner ?: Tuner(session)

        val newNodeTree = tuner.run {
            startComposition()
            runTunable(session)
            endComposition()
        }

        val renderer = Renderer(factories.toMutableList().apply {
            (session.hostView.context as? Activity)?.let {
                add(HibariFactory(it.layoutInflater))
            } ?: add(HibariFactory(LayoutInflater.from(session.hostView.context)))
        }, session.hostView)
        val patcher = Patcher(renderer)

        val oldNodeTree = session.lastTree
        patcher.patch(session.hostView, oldNodeTree, newNodeTree)

        session.memory = tuner.memory
        session.localValueStacks = tuner.localValueStacks
        session.lastTree = newNodeTree
        session.tuner = tuner
    }

    private suspend fun runTuneLoop() {
        while (isRunning) {
            awaitFrame()

            if (invalidations.isNotEmpty()) {
                currentTuneJob?.cancel("A new Tune was scheduled")

                currentTuneJob = scope.launch {
                    val sessionsToTune = drainInvalidations()

                    sessionsToTune.forEach { session ->
                        ensureActive()
                        tuneNow(session)
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
            sessions.add(invalidations.poll()!!)
        }
        return sessions
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitFrame() {
        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Choreographer.getInstance().postFrameCallback {
                    if (continuation.isActive) {
                        continuation.resume(Unit) {}
                    }
                }
            } else {
                mainHandler.postDelayed({
                    if (continuation.isActive) {
                        continuation.resume(Unit) {}
                    }
                }, 16)
            }
        }
    }

}