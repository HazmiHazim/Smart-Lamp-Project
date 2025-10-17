package com.iot.android.smartlamp

import android.app.Application

class SmartLampApp : Application() {
    override fun onCreate() {
        super.onCreate()
        this.deleteDatabase("SmartLamp.db")
        DependencyContainer.initDatabase(this)
    }
}