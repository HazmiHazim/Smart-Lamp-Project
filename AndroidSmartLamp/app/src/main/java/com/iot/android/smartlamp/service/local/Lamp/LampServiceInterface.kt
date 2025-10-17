package com.iot.android.smartlamp.service.local.Lamp

import android.bluetooth.BluetoothDevice
import com.iot.android.smartlamp.model.Lamp

interface LampServiceInterface {
    fun addLamp(lamp : Lamp)
    fun getAllLamps() : List<Lamp>?
    fun getLampById(id : Int) : Lamp?
    fun updateLampState(lampId : Int, state : Boolean)
    fun deleteLamp(id : Int)
    fun connectToLamp(device : BluetoothDevice)
    fun scanForDevice(onFound : (BluetoothDevice) -> Unit)
    fun getConnectedDevice(onFound : (BluetoothDevice?) -> Unit)
    fun turnOnCommand(lampPublicId : String)
    fun turnOffCommand(lampPublicId : String)
    fun setBrightnessCommand(lampPublicId : String, brightness : Int)
    fun setColorCommand(lampPublicId : String, red : Int, green : Int, blue : Int)
    fun disconnect()
    fun onLampUpdate(callback : (List<Lamp>) -> Unit)
}