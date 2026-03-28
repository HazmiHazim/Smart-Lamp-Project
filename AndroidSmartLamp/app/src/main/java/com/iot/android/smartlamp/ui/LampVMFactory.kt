package com.iot.android.smartlamp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iot.android.smartlamp.service.LampServiceInterface

class LampVMFactory(private val lampService: LampServiceInterface) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LampVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LampVM(lampService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
