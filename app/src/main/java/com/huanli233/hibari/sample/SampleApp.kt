package com.huanli233.hibari.sample

import androidx.multidex.MultiDexApplication
import com.google.android.material.color.DynamicColors

class SampleApp: MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

}