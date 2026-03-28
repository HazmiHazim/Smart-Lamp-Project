package com.iot.android.smartlamp

import android.app.Application
import com.iot.android.smartlamp.di.DependencyContainer
import com.iot.android.smartlamp.util.ColorUtils

class SmartLampApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DependencyContainer.initDatabase(this)
        ColorUtils.loadColors(this)
    }
}
