package com.iot.android.smartlamp.viewModel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iot.android.smartlamp.model.Lamp
import com.iot.android.smartlamp.service.local.Lamp.LampServiceInterface

class LampVM(private val lampService : LampServiceInterface) : ViewModel() {

    private val _lampList = MutableLiveData<MutableList<Lamp>>(mutableListOf())
    val lampList : LiveData<MutableList<Lamp>> get() = _lampList

    init {
        // Subscribe to service events for UI updates
        lampService.onLampUpdate { updatedLampList ->
            _lampList.value = updatedLampList.toMutableList()
        }

        // Fetch current data from the service
        val currentList = lampService.getAllLamps()
        _lampList.value = currentList?.toMutableList()
    }

    fun getAllLamps() : List<Lamp>? {
        return lampService.getAllLamps()
    }

    fun addLamp(lamp : Lamp) {
        val currentList = _lampList.value ?: mutableListOf()
        val newList = currentList.toMutableList().apply { add(lamp) }
        _lampList.value = newList
        lampService.addLamp(lamp)
    }

    fun deleteLamp(id : Int) {
        lampService.deleteLamp(id)
    }

    fun toggleLampState(lampId: Int, lampState: Boolean) {
        lampService.updateLampState(lampId, lampState)

        // Update LiveData immediately for UI responsiveness
        _lampList.value = _lampList.value?.map { lamp ->
            if (lamp.id == lampId) lamp.copy(state = lampState) else lamp
        }?.toMutableList()
    }

    fun updateBrightness(lampId : Int, brightness : Int) {
        _lampList.value = _lampList.value?.map { lamp ->
            if (lamp.id == lampId) lamp.copy(brightness = brightness) else lamp
        }?.toMutableList()
    }

    fun connectToLamp(device : BluetoothDevice) {
        lampService.connectToLamp(device)
    }

    fun scanForDevice(onFound : (BluetoothDevice) -> Unit) {
        lampService.scanForDevice(onFound)
    }

    fun getConnectedDevice(onFound : (BluetoothDevice?) -> Unit) {
        lampService.getConnectedDevice(onFound)
    }

    fun turnOnCommand(lampPublicId : String) {
        lampService.turnOnCommand(lampPublicId)
    }

    fun turnOffCommand(lampPublicId : String) {
        lampService.turnOffCommand(lampPublicId)
    }

    fun setBrightnessCommand(lampPublicId : String, brightness : Int) {
        lampService.setBrightnessCommand(lampPublicId, brightness)
    }

    fun setColorCommand(lampPublicId : String, red : Int, green : Int, blue : Int) {
        lampService.setColorCommand(lampPublicId, red, green, blue)
    }

    override fun onCleared() {
        super.onCleared()
        lampService.disconnect()
    }
}
