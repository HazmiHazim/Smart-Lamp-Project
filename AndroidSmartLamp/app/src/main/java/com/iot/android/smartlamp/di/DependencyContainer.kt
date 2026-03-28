package com.iot.android.smartlamp.di

import android.content.Context
import com.iot.android.smartlamp.data.local.AppDatabase
import com.iot.android.smartlamp.data.local.LampRepository
import com.iot.android.smartlamp.data.local.LampRepositoryInterface
import com.iot.android.smartlamp.data.ble.BluetoothManager
import com.iot.android.smartlamp.data.ble.BluetoothManagerInterface
import com.iot.android.smartlamp.service.LampService
import com.iot.android.smartlamp.service.LampServiceInterface

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
