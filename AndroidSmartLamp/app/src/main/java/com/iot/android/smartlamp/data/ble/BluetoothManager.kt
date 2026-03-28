package com.iot.android.smartlamp.data.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.iot.android.smartlamp.model.DiscoveredLed
import com.iot.android.smartlamp.model.LampBluetoothAddress
import java.util.LinkedList

class BluetoothManager(private val context: Context) : BluetoothManagerInterface {

    private var bluetoothGatt: BluetoothGatt? = null
    private val lampBLEAddressMap = mutableMapOf<String, LampBluetoothAddress>()
    override var onLedsDiscovered: ((List<DiscoveredLed>) -> Unit)? = null
    override var onConnectionStateChanged: ((connected: Boolean) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scanHandler = Handler(Looper.getMainLooper())
    private var currentScanCallback: ScanCallback? = null

    private val writeQueue = LinkedList<WriteRequest>()
    private var isWriting = false
    private val writeTimeoutRunnable = Runnable {
        Log.e("BLE", "Write timed out, unblocking queue")
        processNextWrite()
    }

    private data class WriteRequest(
        val characteristic: BluetoothGattCharacteristic,
        val data: ByteArray,
        val gatt: BluetoothGatt
    )

    private val prefs by lazy {
        context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    }

    private val adapter by lazy {
        val manager = context.getSystemService(BluetoothManager::class.java)
        manager.adapter
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(device: BluetoothDevice) {
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
            gatt.close()
            bluetoothGatt = null
        }

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
                        synchronized(writeQueue) {
                            writeQueue.clear()
                            isWriting = false
                        }
                        mainHandler.removeCallbacks(writeTimeoutRunnable)
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
                        mainHandler.post { onLedsDiscovered?.invoke(discoveredLeds) }
                        return
                    }
                }

                Log.e("BLE", "No compatible service found with RX/TX characteristics")
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                mainHandler.removeCallbacks(writeTimeoutRunnable)
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e("BLE", "Write failed for ${characteristic.uuid}, status: $status")
                } else {
                    Log.d("BLE", "Write success for ${characteristic.uuid}")
                }
                processNextWrite()
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
            Log.e("BLE", "Available mappings: ${lampBLEAddressMap.keys}")
            Log.e("BLE", "GATT connected: ${bluetoothGatt != null}")
            return
        }

        val gatt = bluetoothGatt ?: run {
            Log.e("BLE", "GATT not connected")
            return
        }

        val char = gatt.getService(address.serviceAddress)?.getCharacteristic(address.receiverAddress)
        if (char != null) {
            Log.d("COMMAND", "Queuing write to ${address.receiverAddress}: ${String(data)}")
            enqueueWrite(WriteRequest(char, data, gatt))
        } else {
            Log.e("BLE", "Characteristic not available for ${address.receiverAddress}")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enqueueWrite(request: WriteRequest) {
        synchronized(writeQueue) {
            writeQueue.add(request)
            if (!isWriting) {
                isWriting = true
                executeWrite(request)
            }
        }
    }

    private fun processNextWrite() {
        synchronized(writeQueue) {
            writeQueue.poll()
            val next = writeQueue.peek()
            if (next != null) {
                executeWrite(next)
            } else {
                isWriting = false
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun executeWrite(request: WriteRequest) {
        val result = request.gatt.writeCharacteristic(
            request.characteristic,
            request.data,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )

        if (result != BluetoothStatusCodes.SUCCESS) {
            Log.e("BLE", "writeCharacteristic returned error: $result, retrying in 100ms")
            mainHandler.postDelayed({
                val retry = request.gatt.writeCharacteristic(
                    request.characteristic,
                    request.data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                if (retry != BluetoothStatusCodes.SUCCESS) {
                    Log.e("BLE", "Retry also failed: $retry, skipping")
                    processNextWrite()
                } else {
                    mainHandler.postDelayed(writeTimeoutRunnable, 5000)
                }
            }, 100)
        } else {
            mainHandler.postDelayed(writeTimeoutRunnable, 5000)
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
        mainHandler.removeCallbacks(writeTimeoutRunnable)
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        lampBLEAddressMap.clear()
        synchronized(writeQueue) {
            writeQueue.clear()
            isWriting = false
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun scanEsp32Device(onFound: (BluetoothDevice) -> Unit) {
        val scanner = adapter.bluetoothLeScanner ?: run {
            Log.e("BLE", "BLE scanner not available")
            return
        }

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

            mainHandler.postDelayed({
                if (bluetoothGatt != null && lampBLEAddressMap.isEmpty()) {
                    Log.d("BLE", "Reconnect timed out after 10 seconds")
                    bluetoothGatt?.disconnect()
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }, 10_000)

            true
        } catch (e: IllegalArgumentException) {
            Log.e("BLE", "Invalid MAC address: $mac")
            prefs.edit().remove("last_device_mac").apply()
            false
        }
    }
}
