package com.iot.android.smartlamp.service.local.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback

interface BluetoothManagerInterface {
    fun connect(device: BluetoothDevice)
    fun writeData(lampPublicId : String, data : ByteArray)
    fun enableNotifications(lampPublicId : String)
    fun disconnect()
    var eventListener: ((publicId : String) -> Unit)?
    fun scanEsp32Device(onFound: (BluetoothDevice) -> Unit)
    fun getConnectedEsp32(onFound: (BluetoothDevice?) -> Unit)
}