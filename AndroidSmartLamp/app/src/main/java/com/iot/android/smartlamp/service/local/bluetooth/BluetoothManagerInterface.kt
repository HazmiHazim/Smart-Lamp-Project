package com.iot.android.smartlamp.service.local.bluetooth

import android.bluetooth.BluetoothDevice
import com.iot.android.smartlamp.model.DiscoveredLed

interface BluetoothManagerInterface {
    fun connect(device: BluetoothDevice)
    fun writeData(lampPublicId : String, data : ByteArray)
    fun enableNotifications(lampPublicId : String)
    fun disconnect()
    var onLedsDiscovered: ((List<DiscoveredLed>) -> Unit)?
    fun scanEsp32Device(onFound: (BluetoothDevice) -> Unit)
    fun getConnectedEsp32(onFound: (BluetoothDevice?) -> Unit)
    fun reconnectLastDevice(): Boolean
    fun registerLampMapping(lampPublicId: String, led: DiscoveredLed)
}
