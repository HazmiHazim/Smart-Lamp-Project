package com.iot.android.smartlamp.service.local.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.iot.android.smartlamp.model.DiscoveredLed
import com.iot.android.smartlamp.model.LampBluetoothAddress

class BluetoothManager(private val context: Context) : BluetoothManagerInterface {

    private var bluetoothGatt: BluetoothGatt? = null
    private val lampBLEAddressMap = mutableMapOf<String, LampBluetoothAddress>()
    override var onLedsDiscovered: ((List<DiscoveredLed>) -> Unit)? = null
    override var onConnectionStateChanged: ((connected: Boolean) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scanHandler = Handler(Looper.getMainLooper())
    private var currentScanCallback: ScanCallback? = null

    private val prefs by lazy {
        context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    }

    private val adapter by lazy {
        val manager = context.getSystemService(BluetoothManager::class.java)
        manager.adapter
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(device: BluetoothDevice) {
        // Close existing connection before creating a new one
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
            gatt.close()
            bluetoothGatt = null
        }

        // Save MAC for auto-reconnect
        prefs.edit().putString("last_device_mac", device.address).apply()

        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d("BLE", "Connected to GATT server")
                        mainHandler.post { onConnectionStateChanged?.invoke(true) }
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d("BLE", "Disconnected from GATT server")
                        gatt.close()
                        bluetoothGatt = null
                        lampBLEAddressMap.clear()
                        mainHandler.post { onConnectionStateChanged?.invoke(false) }
                    }
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
                        // Dispatch to main thread
                        mainHandler.post { onLedsDiscovered?.invoke(discoveredLeds) }
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
        val address = lampBLEAddressMap[lampPublicId] ?: run {
            Log.e("BLE", "LED not found: $lampPublicId")
            return
        }

        val gatt = bluetoothGatt ?: run {
            Log.e("BLE", "GATT not connected")
            return
        }

        val char = gatt.getService(address.serviceAddress)?.getCharacteristic(address.receiverAddress)
        if (char != null) {
            Log.d("COMMAND", "Writing to ${address.receiverAddress}: ${data.contentToString()}")
            gatt.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            Log.e("BLE", "Characteristic not available")
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
        lampBLEAddressMap.clear()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun scanEsp32Device(onFound: (BluetoothDevice) -> Unit) {
        val scanner = adapter.bluetoothLeScanner ?: run {
            Log.e("BLE", "BLE scanner not available")
            return
        }

        // Stop any ongoing scan
        currentScanCallback?.let { scanner.stopScan(it) }

        val scanCallback = object : ScanCallback() {
            @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val advertisedName = result.scanRecord?.deviceName
                Log.d("BLE", "Device found: $advertisedName")
                if (advertisedName?.contains("ESP32", ignoreCase = true) == true) {
                    scanner.stopScan(this)
                    scanHandler.removeCallbacksAndMessages(null)
                    currentScanCallback = null
                    onFound(device)
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
            override fun onScanFailed(errorCode: Int) {
                scanner.stopScan(this)
                scanHandler.removeCallbacksAndMessages(null)
                currentScanCallback = null
                Log.e("BLE", "Scan failed: $errorCode")
            }
        }

        currentScanCallback = scanCallback
        scanner.startScan(scanCallback)

        // Timeout after 10 seconds
        scanHandler.postDelayed({
            scanner.stopScan(scanCallback)
            currentScanCallback = null
            Log.d("BLE", "Scan timed out after 10 seconds")
        }, 10_000)
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
