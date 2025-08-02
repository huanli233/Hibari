package com.huanli233.hibari.runtime

import android.app.Activity
import android.view.LayoutInflater
import com.huanli233.hibari.ui.HibariFactory

object TuneController {

    fun tune(session: Tunation, factories: List<HibariFactory> = emptyList()) {
        val tuner = session.tuner ?: Tuner(session)
        session.tuner = tuner

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

        session.lastTree = newNodeTree
        session.tuneData = tuner.getTuneData()
    }
}