package com.iot.android.smartlamp.service

import android.bluetooth.BluetoothDevice
import com.iot.android.smartlamp.model.Lamp

interface LampServiceInterface {
    fun addLamp(lamp : Lamp)
    fun getAllLamps() : List<Lamp>?
    fun getLampById(id : Int) : Lamp?
    fun updateLampState(lampId : Int, state : Boolean)
    fun updateLampColour(lampId : Int, colour : String)
    fun deleteLamp(id : Int)
    fun connectToLamp(device : BluetoothDevice)
    fun scanForDevice(onFound : (BluetoothDevice) -> Unit, onScanComplete: () -> Unit = {})
    fun getConnectedDevice(onFound : (BluetoothDevice?) -> Unit)
    fun reconnectLastDevice(): Boolean
    fun turnOnCommand(lampPublicId : String)
    fun turnOffCommand(lampPublicId : String)
    fun setBrightnessCommand(lampPublicId : String, brightness : Int)
    fun setColorCommand(lampPublicId : String, red : Int, green : Int, blue : Int)
    fun disconnect()
    fun onLampUpdate(callback : (List<Lamp>) -> Unit)
}
