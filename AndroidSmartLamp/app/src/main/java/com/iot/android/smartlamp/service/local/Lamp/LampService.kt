package com.iot.android.smartlamp.service.local.Lamp

import android.bluetooth.BluetoothDevice
import com.iot.android.smartlamp.data.repository.LampRepositoryInterface
import com.iot.android.smartlamp.model.Lamp
import com.iot.android.smartlamp.service.local.bluetooth.BluetoothManagerInterface

class LampService(
    private val bluetoothManager: BluetoothManagerInterface,
    private val lampRepo: LampRepositoryInterface
) : LampServiceInterface {

    private var updateCallback: ((List<Lamp>) -> Unit)? = null

    init {
        bluetoothManager.eventListener = { publicId ->
            val updatedList = lampRepo.getAllLamps().map { it.copy(publicId = publicId) }
            updateCallback?.invoke(updatedList)
        }
    }

    override fun addLamp(lamp : Lamp) {
        lampRepo.insertLamp(lamp)
        updateCallback?.invoke(lampRepo.getAllLamps())
    }

    override fun getLampById(id : Int) : Lamp? {
        return lampRepo.getLampById(id)
    }

    override fun getAllLamps() : List<Lamp>? {
        return lampRepo.getAllLamps()
    }

    override fun updateLampState(lampId : Int, state : Boolean) {
        lampRepo.updateLampState(lampId, state)
        updateCallback?.invoke(lampRepo.getAllLamps()) // Notify UI observers
    }

    override fun deleteLamp(id : Int) {
        lampRepo.deleteLamp(id)
        updateCallback?.invoke(lampRepo.getAllLamps())
    }

    override fun connectToLamp(device : BluetoothDevice) {
        bluetoothManager.connect(device)
    }

    override fun scanForDevice(onFound : (BluetoothDevice) -> Unit) {
        bluetoothManager.scanEsp32Device(onFound)
    }

    override fun getConnectedDevice(onFound : (BluetoothDevice?) -> Unit) {
        bluetoothManager.getConnectedEsp32 { device ->
            onFound(device)
        }
    }

    override fun turnOnCommand(lampPublicId : String) {
        val brightness = "255".toByteArray(Charsets.UTF_8)
        bluetoothManager.writeData(lampPublicId, brightness)
    }

    override fun turnOffCommand(lampPublicId : String) {
        val brightness = "0".toByteArray(Charsets.UTF_8)
        bluetoothManager.writeData(lampPublicId, brightness)
    }

    override fun setBrightnessCommand(lampPublicId : String, brightness : Int) {
        bluetoothManager.writeData(lampPublicId, brightness.toString().toByteArray(Charsets.UTF_8))
    }

    override fun setColorCommand(lampPublicId : String, red : Int, green : Int, blue : Int) {
        bluetoothManager.writeData(lampPublicId, byteArrayOf(red.toByte(), green.toByte(), blue.toByte()))
    }

    override fun disconnect() {
        bluetoothManager.disconnect()
    }

    override fun onLampUpdate(callback : (List<Lamp>) -> Unit) {
        updateCallback = callback
    }
}