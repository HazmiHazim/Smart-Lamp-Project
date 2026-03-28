package com.iot.android.smartlamp.viewModel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.android.smartlamp.model.Lamp
import com.iot.android.smartlamp.service.local.Lamp.LampServiceInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LampVM(private val lampService : LampServiceInterface) : ViewModel() {

    private val _lampList = MutableLiveData<List<Lamp>>(emptyList())
    val lampList : LiveData<List<Lamp>> get() = _lampList

    init {
        lampService.onLampUpdate { updatedLampList ->
            _lampList.postValue(updatedLampList.toList())
        }

        viewModelScope.launch {
            val currentList = withContext(Dispatchers.IO) {
                lampService.getAllLamps()
            }
            _lampList.postValue(currentList?.toList() ?: emptyList())
        }
    }

    fun addLamp(lamp : Lamp) {
        viewModelScope.launch(Dispatchers.IO) {
            lampService.addLamp(lamp)
        }
    }

    fun deleteLamp(id : Int) {
        viewModelScope.launch(Dispatchers.IO) {
            lampService.deleteLamp(id)
        }
    }

    fun toggleLampState(lampId: Int, lampState: Boolean) {
        // Update UI immediately
        _lampList.postValue(_lampList.value?.map { lamp ->
            if (lamp.id == lampId) lamp.copy(state = lampState) else lamp
        } ?: emptyList())

        viewModelScope.launch(Dispatchers.IO) {
            lampService.updateLampState(lampId, lampState)
        }
    }

    fun updateBrightness(lampId : Int, brightness : Int) {
        _lampList.postValue(_lampList.value?.map { lamp ->
            if (lamp.id == lampId) lamp.copy(brightness = brightness) else lamp
        } ?: emptyList())
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

    fun reconnectLastDevice() {
        lampService.reconnectLastDevice()
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
