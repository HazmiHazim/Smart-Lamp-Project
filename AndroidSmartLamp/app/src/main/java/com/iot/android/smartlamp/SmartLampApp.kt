package com.iot.android.smartlamp

import android.app.Application

class SmartLampApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DependencyContainer.initDatabase(this)
    }
}
