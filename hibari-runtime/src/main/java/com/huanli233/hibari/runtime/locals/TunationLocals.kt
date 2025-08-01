package com.huanli233.hibari.runtime.locals

import android.content.res.Configuration
import androidx.lifecycle.LifecycleOwner
import com.huanli233.hibari.runtime.staticTunationLocalOf
import com.huanli233.hibari.ui.unit.Density
import com.huanli233.hibari.ui.unit.LayoutDirection

val LocalLifecycleOwner = staticTunationLocalOf<LifecycleOwner> {
    error("TunationLocal LocalLifecycleOwner not present")
}
val LocalConfiguration = staticTunationLocalOf<Configuration> {
    error("TunationLocal LocalConfiguration not present")
}
val LocalDensity = staticTunationLocalOf<Density> {
    error("TunationLocal LocalDensity not present")
}
val LocalLayoutDirection = staticTunationLocalOf<LayoutDirection> {
    error("TunationLocal LocalDensity not present")
}