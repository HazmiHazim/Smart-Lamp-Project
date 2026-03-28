package com.iot.android.smartlamp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.di.DependencyContainer
import com.iot.android.smartlamp.ui.home.Home
import kotlin.getValue

class MainActivity : AppCompatActivity() {
    private val lampVM: LampVM by viewModels {
        LampVMFactory(DependencyContainer.provideLampService(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        checkBluetoothPermission()

        if (savedInstanceState == null) {
            supportActionBar?.hide()
            supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragment_container, Home()).commit()
        }
    }

    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value == true }
            if (allGranted) {
                onBluetoothPermissionGranted()
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth permissions are required to use this feature",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private fun checkBluetoothPermission() {
        val bluetoothPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        val missingPermissions = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestBluetoothPermissions.launch(missingPermissions.toTypedArray())
        } else {
            onBluetoothPermissionGranted()
        }
    }

    private fun onBluetoothPermissionGranted() {
        lampVM.reconnectLastDevice()
    }
}
