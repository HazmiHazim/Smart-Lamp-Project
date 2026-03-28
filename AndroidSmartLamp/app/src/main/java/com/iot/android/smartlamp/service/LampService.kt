package com.iot.android.smartlamp.service

import android.bluetooth.BluetoothDevice
import com.iot.android.smartlamp.data.local.LampRepositoryInterface
import com.iot.android.smartlamp.data.ble.BluetoothManagerInterface
import com.iot.android.smartlamp.model.DiscoveredLed
import com.iot.android.smartlamp.model.Lamp
import java.util.concurrent.Executors

class LampService(
    private val bluetoothManager: BluetoothManagerInterface,
    private val lampRepo: LampRepositoryInterface
) : LampServiceInterface {

    private var updateCallback: ((List<Lamp>) -> Unit)? = null
    private val dbExecutor = Executors.newSingleThreadExecutor()

    init {
        bluetoothManager.onLedsDiscovered = { discoveredLeds ->
            dbExecutor.execute {
                registerDiscoveredLamps(discoveredLeds)
            }
        }
    }

    private fun registerDiscoveredLamps(leds: List<DiscoveredLed>) {
        val existingLamps = lampRepo.getAllLamps()
        val existingRxKeys = existingLamps.map { it.rxKey }.toSet()

        for ((index, led) in leds.withIndex()) {
            val rxKey = led.rxUUID.toString()

            if (rxKey !in existingRxKeys) {
                val lamp = Lamp(
                    publicId = rxKey,
                    name = "LED ${index + 1}",
                    model = "ESP32-Smart-Lamp",
                    state = false,
                    colour = "No Colour Yet",
                    txKey = led.txUUID.toString(),
                    rxKey = rxKey
                )
                lampRepo.insertLamp(lamp)
            }

            bluetoothManager.registerLampMapping(rxKey, led)
        }

        val allLamps = lampRepo.getAllLamps()
        for (lamp in allLamps) {
            val matchingLed = leds.find { it.rxUUID.toString() == lamp.rxKey }
            if (matchingLed != null) {
                bluetoothManager.registerLampMapping(lamp.rxKey, matchingLed)
            }
        }

        updateCallback?.invoke(allLamps)
    }

    override fun addLamp(lamp: Lamp) {
        lampRepo.insertLamp(lamp)
        updateCallback?.invoke(lampRepo.getAllLamps())
    }

    override fun getLampById(id: Int): Lamp? {
        return lampRepo.getLampById(id)
    }

    override fun getAllLamps(): List<Lamp>? {
        return lampRepo.getAllLamps()
    }

    override fun updateLampState(lampId: Int, state: Boolean) {
        lampRepo.updateLampState(lampId, state)
        updateCallback?.invoke(lampRepo.getAllLamps())
    }

    override fun deleteLamp(id: Int) {
        lampRepo.deleteLamp(id)
        updateCallback?.invoke(lampRepo.getAllLamps())
    }

    override fun connectToLamp(device: BluetoothDevice) {
        bluetoothManager.connect(device)
    }

    override fun scanForDevice(onFound: (BluetoothDevice) -> Unit) {
        bluetoothManager.scanEsp32Device(onFound)
    }

    override fun getConnectedDevice(onFound: (BluetoothDevice?) -> Unit) {
        bluetoothManager.getConnectedEsp32 { device ->
            onFound(device)
        }
    }

    override fun reconnectLastDevice(): Boolean {
        return bluetoothManager.reconnectLastDevice()
    }

    override fun turnOnCommand(lampPublicId: String) {
        val brightness = "255".toByteArray(Charsets.UTF_8)
        bluetoothManager.writeData(lampPublicId, brightness)
    }

    override fun turnOffCommand(lampPublicId: String) {
        val brightness = "0".toByteArray(Charsets.UTF_8)
        bluetoothManager.writeData(lampPublicId, brightness)
    }

    override fun setBrightnessCommand(lampPublicId: String, brightness: Int) {
        bluetoothManager.writeData(lampPublicId, brightness.toString().toByteArray(Charsets.UTF_8))
    }

    override fun setColorCommand(lampPublicId: String, red: Int, green: Int, blue: Int) {
        bluetoothManager.writeData(lampPublicId, byteArrayOf(red.toByte(), green.toByte(), blue.toByte()))
    }

    override fun disconnect() {
        bluetoothManager.disconnect()
    }

    override fun onLampUpdate(callback: (List<Lamp>) -> Unit) {
        updateCallback = callback
    }
}
