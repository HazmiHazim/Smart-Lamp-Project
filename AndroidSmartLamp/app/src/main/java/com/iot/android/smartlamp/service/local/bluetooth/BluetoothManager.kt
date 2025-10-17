package com.iot.android.smartlamp.service.local.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.iot.android.smartlamp.data.repository.LampRepositoryInterface
import com.iot.android.smartlamp.model.LampBluetoothAddress
import java.util.UUID

class BluetoothManager(private val context : Context, private val lampRepo: LampRepositoryInterface) : BluetoothManagerInterface {

    private var bluetoothGatt: BluetoothGatt? = null
    private val lampBLEAddressMap = mutableMapOf<String, LampBluetoothAddress>()
    override var eventListener: ((serviceKey: String) -> Unit)? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(device : BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BLE", "Connected to GATT server")
                    gatt.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE", "Services discovered, count: ${gatt.services.size}")

                    var foundService: BluetoothGattService? = null
                    var rx: UUID? = null
                    var tx: UUID? = null

                    // First, find the correct service and characteristics
                    for (service in gatt.services) {
                        Log.d("BLE", "Service: ${service.uuid}")

                        var serviceRx: UUID? = null
                        var serviceTx: UUID? = null

                        service.characteristics.forEach { characteristic ->
                            Log.d("BLE", "  Characteristic: ${characteristic.uuid}, Properties: ${characteristic.properties}")
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                                serviceRx = characteristic.uuid
                                Log.d("BLE", "    Found RX characteristic: $serviceRx")
                            } else if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                                serviceTx = characteristic.uuid
                                Log.d("BLE", "    Found TX characteristic: $serviceTx")
                            }
                        }

                        if (serviceRx != null && serviceTx != null) {
                            foundService = service
                            rx = serviceRx
                            tx = serviceTx
                            Log.d("BLE", "Found compatible service: ${service.uuid}")
                            break // Found what we need, stop searching
                        }
                    }

                    // Only populate the map if we found a valid service
                    if (foundService != null && rx != null && tx != null) {
                        val lamps = lampRepo.getAllLamps()
                        Log.d("BLE", "Populating map with ${lamps.size} lamps")

                        lamps.forEach { lamp ->
                            lampBLEAddressMap[lamp.publicId] = LampBluetoothAddress(
                                lampId = lamp.publicId,
                                serviceAddress = foundService.uuid,
                                transmitterAddress = tx,
                                receiverAddress = rx
                            )
                            Log.d("BLE", "Map entry: ${lamp.publicId} -> ${foundService.uuid} / $tx / $rx")
                        }

                        Log.d("BLE", "Map population complete. Total entries: ${lampBLEAddressMap.size}")
                        eventListener?.invoke(foundService.uuid.toString())
                    } else {
                        Log.e("BLE", "No compatible service found with both RX and TX characteristics")
                    }
                } else {
                    Log.e("BLE", "Service discovery failed with status: $status")
                }
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun writeData(lampPublicId : String, data : ByteArray) {
        Log.d("BLE", "Map keys before write: ${lampBLEAddressMap.keys}")
        val address = lampBLEAddressMap[lampPublicId] ?: run {
            Log.e("BLE", "LED not found: $lampBLEAddressMap")
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
    override fun enableNotifications(lampPublicId : String) {
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

        // No ESP32 found
        Log.d("BLE", "No connected ESP32 device found")
        onFound(null)
    }
}