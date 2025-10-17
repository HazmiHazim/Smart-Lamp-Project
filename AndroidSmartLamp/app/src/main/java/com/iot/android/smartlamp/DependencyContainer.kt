package com.iot.android.smartlamp

import android.content.Context
import com.iot.android.smartlamp.data.AppDatabase
import com.iot.android.smartlamp.data.repository.LampRepository
import com.iot.android.smartlamp.data.repository.LampRepositoryInterface
import com.iot.android.smartlamp.service.local.Lamp.LampService
import com.iot.android.smartlamp.service.local.Lamp.LampServiceInterface
import com.iot.android.smartlamp.service.local.bluetooth.BluetoothManager
import com.iot.android.smartlamp.service.local.bluetooth.BluetoothManagerInterface

object DependencyContainer {
    private var appDatabase : AppDatabase? = null
    private var bluetoothManager : BluetoothManagerInterface? = null
    private var lampService: LampServiceInterface? = null

    fun initDatabase(context : Context) {
        appDatabase = AppDatabase.getInstance(context)
    }

    private val lampRepository : LampRepositoryInterface by lazy {
        LampRepository(appDatabase!!)
    }

    private fun provideBluetoothManager(context:  Context) : BluetoothManagerInterface {
        if (bluetoothManager == null) {
            bluetoothManager = BluetoothManager(context.applicationContext, lampRepository)
        }
        return bluetoothManager!!
    }

    fun provideLampService(context: Context) : LampServiceInterface {
        if (lampService == null) {
            val bluetoothManager = provideBluetoothManager(context)
            lampService = LampService(bluetoothManager, lampRepository)
        }
        return lampService!!
    }
}