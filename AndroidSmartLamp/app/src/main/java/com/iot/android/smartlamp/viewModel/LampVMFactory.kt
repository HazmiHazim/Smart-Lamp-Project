package com.iot.android.smartlamp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iot.android.smartlamp.service.local.Lamp.LampServiceInterface

class LampVMFactory(private val lampService: LampServiceInterface) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LampVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LampVM(lampService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
