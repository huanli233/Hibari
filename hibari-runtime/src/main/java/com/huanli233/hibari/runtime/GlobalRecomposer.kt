package com.huanli233.hibari.runtime

import kotlinx.coroutines.Dispatchers

object GlobalRecomposer {
    val retuner: Retuner by lazy { Retuner(Dispatchers.Main) }

    fun scheduleTune(session: Tunation) {
        retuner.scheduleTune(session)
    }
}