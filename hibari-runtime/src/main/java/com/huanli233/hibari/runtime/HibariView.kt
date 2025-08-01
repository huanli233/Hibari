package com.huanli233.hibari.runtime

import android.content.Context
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.highcapable.betterandroid.ui.extension.component.lifecycleOwner
import com.huanli233.hibari.runtime.locals.LocalConfiguration
import com.huanli233.hibari.runtime.locals.LocalDensity
import com.huanli233.hibari.runtime.locals.LocalLayoutDirection
import com.huanli233.hibari.runtime.locals.LocalLifecycleOwner
import com.huanli233.hibari.ui.unit.Density
import com.huanli233.hibari.ui.unit.LayoutDirection

class HibariView @JvmOverloads constructor(
    context: Context,
    content: @Tunable () -> Unit = {}
): FrameLayout(context) {

    var tunation = Tunation(this, content = { runTunable(content) })
        private set

    init {
        GlobalRetuner.retuner.scheduleRetune(tunation)
    }

    fun setContent(content: @Tunable () -> Unit = {}) {
        synchronized(this) {
            tunation.dispose()
            tunation = Tunation(this, content = { runTunable(content) })
            GlobalRetuner.retuner.scheduleRetune(tunation)
        }
    }

    var density by mutableStateOf(Density(context), referentialEqualityPolicy())
        private set
    var configuration by mutableStateOf(Configuration(context.resources.configuration))
    @get:JvmName("getHibariLayoutDirection") @set:JvmName("setHibariLayoutDirection")
    var layoutDirection by mutableStateOf(ViewCompat.getLayoutDirection(this))

    @Tunable
    fun runTunable(content: @Tunable () -> Unit) {
        TunationLocalProvider(
            *listOfNotNull(
                lifecycleOwner?.let { LocalLifecycleOwner provides it },
                LocalContext provides context,
                LocalConfiguration provides configuration,
                LocalDensity provides density,
                LocalLayoutDirection provides layoutDirection.let {
                    when (it) {
                        ViewCompat.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
                        else -> LayoutDirection.Ltr
                    }
                }
            ).toTypedArray()
        ) {
            content()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        density = Density(context)
        configuration = Configuration(newConfig)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        tunation.dispose()
    }

}