package com.iot.android.smartlamp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.di.DependencyContainer
import com.iot.android.smartlamp.service.voice.VoiceRecognitionManager
import com.iot.android.smartlamp.ui.home.Home
import kotlin.getValue

class MainActivity : AppCompatActivity() {
    private val lampVM: LampVM by viewModels {
        LampVMFactory(DependencyContainer.provideLampService(this))
    }

    private lateinit var voiceManager: VoiceRecognitionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.main)

        voiceManager = DependencyContainer.provideVoiceRecognitionManager(this)

        checkPermissions()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragment_container, Home()).commit()
        }
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value == true }
            if (allGranted) {
                onAllPermissionsGranted()
            } else {
                val bleScan = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
                val bleConnect = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
                val audio = permissions[Manifest.permission.RECORD_AUDIO] ?: false

                if (bleScan && bleConnect) {
                    onBluetoothPermissionGranted()
                }
                if (!audio) {
                    Toast.makeText(this, "Microphone permission required for voice control", Toast.LENGTH_LONG).show()
                }
                if (!bleScan || !bleConnect) {
                    Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show()
                }
            }
        }

    private fun checkPermissions() {
        val allPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.RECORD_AUDIO
        )

        val missingPermissions = allPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        Log.d("MainActivity", "checkPermissions: missing=${missingPermissions}")

        if (missingPermissions.isNotEmpty()) {
            requestPermissions.launch(missingPermissions.toTypedArray())
        } else {
            onAllPermissionsGranted()
        }
    }

    private fun onAllPermissionsGranted() {
        Log.d("MainActivity", "onAllPermissionsGranted called")
        onBluetoothPermissionGranted()
        // Delay voice init so Home fragment has time to set up callbacks
        window.decorView.post {
            Log.d("MainActivity", "Calling voiceManager.initialize()")
            voiceManager.initialize()
        }
    }

    private fun onBluetoothPermissionGranted() {
        lampVM.reconnectLastDevice()
        val homeFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? Home
        homeFragment?.onPermissionGranted()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            voiceManager.stop()
            lampVM.disconnect()
        }
    }
}
