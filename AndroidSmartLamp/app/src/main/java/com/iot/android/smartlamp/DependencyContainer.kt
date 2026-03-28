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

    @Volatile
    private var bluetoothManager : BluetoothManagerInterface? = null

    @Volatile
    private var lampService: LampServiceInterface? = null

    fun initDatabase(context : Context) {
        appDatabase = AppDatabase.getInstance(context)
    }

    private val lampRepository : LampRepositoryInterface by lazy {
        LampRepository(appDatabase!!)
    }

    private fun provideBluetoothManager(context: Context) : BluetoothManagerInterface {
        return bluetoothManager ?: synchronized(this) {
            bluetoothManager ?: BluetoothManager(context.applicationContext).also {
                bluetoothManager = it
            }
        }
    }

    fun provideLampService(context: Context) : LampServiceInterface {
        return lampService ?: synchronized(this) {
            lampService ?: LampService(
                provideBluetoothManager(context),
                lampRepository
            ).also {
                lampService = it
            }
        }
    }
}
