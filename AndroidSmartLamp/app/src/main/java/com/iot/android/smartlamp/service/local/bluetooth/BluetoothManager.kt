package com.iot.android.smartlamp.service.local.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.iot.android.smartlamp.model.DiscoveredLed
import com.iot.android.smartlamp.model.LampBluetoothAddress
import java.util.UUID

class BluetoothManager(private val context: Context) : BluetoothManagerInterface {

    private var bluetoothGatt: BluetoothGatt? = null
    private val lampBLEAddressMap = mutableMapOf<String, LampBluetoothAddress>()
    override var onLedsDiscovered: ((List<DiscoveredLed>) -> Unit)? = null

    private val prefs by lazy {
        context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(device: BluetoothDevice) {
        // Save MAC for auto-reconnect
        prefs.edit().putString("last_device_mac", device.address).apply()

        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BLE", "Connected to GATT server")
                    gatt.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e("BLE", "Service discovery failed with status: $status")
                    return
                }

                Log.d("BLE", "Services discovered, count: ${gatt.services.size}")

                for (service in gatt.services) {
                    Log.d("BLE", "Service: ${service.uuid}")

                    val rxChars = mutableListOf<BluetoothGattCharacteristic>()
                    val txChars = mutableListOf<BluetoothGattCharacteristic>()

                    for (characteristic in service.characteristics) {
                        Log.d("BLE", "  Characteristic: ${characteristic.uuid}, Properties: ${characteristic.properties}")
                        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                            rxChars.add(characteristic)
                        } else if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                            txChars.add(characteristic)
                        }
                    }

                    // ESP32 firmware creates pairs in order: tx0, rx0, tx1, rx1, tx2, rx2
                    // So txChars and rxChars should have matching indices
                    if (rxChars.isNotEmpty() && txChars.isNotEmpty()) {
                        val pairCount = minOf(rxChars.size, txChars.size)
                        val discoveredLeds = mutableListOf<DiscoveredLed>()

                        for (i in 0 until pairCount) {
                            discoveredLeds.add(
                                DiscoveredLed(
                                    serviceUUID = service.uuid,
                                    rxUUID = rxChars[i].uuid,
                                    txUUID = txChars[i].uuid
                                )
                            )
                            Log.d("BLE", "LED $i: service=${service.uuid}, rx=${rxChars[i].uuid}, tx=${txChars[i].uuid}")
                        }

                        Log.d("BLE", "Discovered $pairCount LED(s)")
                        onLedsDiscovered?.invoke(discoveredLeds)
                        return
                    }
                }

                Log.e("BLE", "No compatible service found with RX/TX characteristics")
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val value = characteristic.value
                Log.d("BLE", "Notification from ${characteristic.uuid}: ${value.contentToString()}")
            }
        })
    }

    override fun registerLampMapping(lampPublicId: String, led: DiscoveredLed) {
        lampBLEAddressMap[lampPublicId] = LampBluetoothAddress(
            lampId = lampPublicId,
            serviceAddress = led.serviceUUID,
            transmitterAddress = led.txUUID,
            receiverAddress = led.rxUUID
        )
        Log.d("BLE", "Registered mapping: $lampPublicId -> rx=${led.rxUUID}")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun writeData(lampPublicId: String, data: ByteArray) {
        Log.d("BLE", "Map keys before write: ${lampBLEAddressMap.keys}")
        val address = lampBLEAddressMap[lampPublicId] ?: run {
            Log.e("BLE", "LED not found: $lampPublicId")
            return
        }

        val char = bluetoothGatt?.getService(address.serviceAddress)?.getCharacteristic(address.receiverAddress)
        if (char != null && bluetoothGatt != null) {
            Log.d("COMMAND", "Command Successful!")
            bluetoothGatt?.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            Log.e("BLE", "Characteristic or GATT not available")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun enableNotifications(lampPublicId: String) {
        val address = lampBLEAddressMap[lampPublicId] ?: return
        val txChar = bluetoothGatt?.getService(address.serviceAddress)?.getCharacteristic(address.transmitterAddress)
        if (txChar != null) {
            bluetoothGatt?.setCharacteristicNotification(txChar, true)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun scanEsp32Device(onFound: (BluetoothDevice) -> Unit) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val scanner = bluetoothAdapter.bluetoothLeScanner
        val scanCallback = object : ScanCallback() {
            @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val scanRecord = result.scanRecord
                val advertisedName = scanRecord?.deviceName
                Log.d("BLE", "Device found: $advertisedName")
                if (advertisedName?.contains("ESP32", ignoreCase = true) == true) {
                    scanner.stopScan(this)
                    onFound(device)
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
            override fun onScanFailed(errorCode: Int) {
                scanner.stopScan(this)
                Log.e("BLE", "Scan failed: $errorCode")
            }
        }
        scanner.startScan(scanCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun getConnectedEsp32(onFound: (BluetoothDevice?) -> Unit) {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)

        if (connectedDevices.isEmpty()) {
            Log.d("BLE", "No connected devices found")
            onFound(null)
            return
        }

        connectedDevices.forEach { device ->
            val deviceName = device.name ?: device.address
            Log.d("BLE", "Connected device: $deviceName")
            if (deviceName.contains("ESP32", ignoreCase = true)) {
                Log.d("BLE", "ESP32 device found: $deviceName")
                onFound(device)
                return
            }
        }

        Log.d("BLE", "No connected ESP32 device found")
        onFound(null)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun reconnectLastDevice(): Boolean {
        val mac = prefs.getString("last_device_mac", null) ?: return false
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false

        return try {
            val device = adapter.getRemoteDevice(mac)
            Log.d("BLE", "Attempting reconnect to $mac")
            connect(device)
            true
        } catch (e: IllegalArgumentException) {
            Log.e("BLE", "Invalid MAC address: $mac")
            prefs.edit().remove("last_device_mac").apply()
            false
        }
    }
}
