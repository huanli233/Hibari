package com.huanli233.hibari.runtime

import android.view.View
import android.view.ViewGroup
import com.huanli233.hibari.runtime.Renderer.Companion.generateViewId
import com.huanli233.hibari.runtime.Renderer.Companion.viewIds
import kotlin.collections.set

fun View.findViewByHibariId(id: String): View? {
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)

            val tagValue = child.getTag(hibariViewId)
            if (tagValue == id) {
                return child
            }

            if (child is ViewGroup && child.getTag(subcomposeLayoutId) != true) {
                val foundView = child.findViewByHibariId(id)
                if (foundView != null) {
                    return foundView
                }
            }
        }
    }

    val tagValue = getTag(hibariViewId)
    if (tagValue == id) {
        return this
    }

    return null
}

@Tunable
fun view(id: String) = currentTuner.tunation.hostView.findViewByHibariId(id) ?: hibariRuntimeError("Cannot find view by id: $id")

@Tunable
fun viewOrNull(id: String) = currentTuner.tunation.hostView.findViewByHibariId(id)

@Tunable
@JvmName("viewTyped")
inline fun <reified V : View> view(id: String) = currentTuner.tunation.hostView.findViewByHibariId(id) as? V ?: hibariRuntimeError("Cannot find view by id: $id")

@Tunable
@JvmName("viewOrNullTyped")
inline fun <reified V : View> viewOrNull(id: String) = currentTuner.tunation.hostView.findViewByHibariId(id) as? V

val String.viewId
    get() = viewIds[this] ?: synchronized(Tuner::class.java) {
        generateViewId(this).second.also {
            viewIds[this] = it
        }
    }