package com.huanli233.hibari.runtime

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
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


    private suspend fun runTuneLoop() {
        while (isRunning) {
            awaitFrame()

            if (invalidations.isNotEmpty()) {
                currentTuneJob?.cancel("A new Tune was scheduled")

                currentTuneJob = scope.launch {
                    val sessionsToTune = drainInvalidations()

                    sessionsToTune.forEach { session ->
                        ensureActive()
                        val tuner = Tuner()
                        tuner.memory = session.memory
                        val newNodeList = tuner.run {
                            runTunable(session)
                            nodes
                        }

                        val renderer = Renderer(factories.toMutableList().apply {
                            (session.hostView.context as? Activity)?.let {
                                add(HibariFactory(it.layoutInflater))
                            } ?: add(HibariFactory(LayoutInflater.from(session.hostView.context)))
                        }, session.hostView)

                        val updateCallback = object : ListUpdateCallback {
                            override fun onInserted(position: Int, count: Int) {
                                session.hostView.addView(
                                    renderer.render(newNodeList[position]), position)
                            }
                            override fun onRemoved(position: Int, count: Int) {
                                session.hostView.removeViewAt(position)
                            }

                            override fun onMoved(fromPosition: Int, toPosition: Int) {
                                val view = session.hostView.getChildAt(fromPosition)
                                session.hostView.removeViewAt(fromPosition)
                                session.hostView.addView(view, toPosition)
                            }

                            override fun onChanged(position: Int, count: Int, payload: Any?) {
                                if (payload != null  && payload is HibariDiffCallback.ModifierChangePayload) {
                                    val view = session.hostView.getChildAt(position)
                                    renderer.applyAttributes(view, payload.changedAttributes)
                                } else {
                                    for (i in (count - 1) downTo 0) {
                                        val currentPos = position + i
                                        session.hostView.removeViewAt(currentPos)
                                        session.hostView.addView(renderer.render(newNodeList[currentPos]), currentPos)
                                    }
                                }
                            }
                        }

                        val diffResult = DiffUtil.calculateDiff(HibariDiffCallback(session.previousNodeList, newNodeList))
                        diffResult.dispatchUpdatesTo(updateCallback)

                        session.memory = tuner.memory
                        session.previousNodeList = newNodeList
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
        while(invalidations.isNotEmpty()) {
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