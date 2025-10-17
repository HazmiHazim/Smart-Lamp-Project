package com.iot.android.smartlamp.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.iot.android.smartlamp.DependencyContainer
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.screen.Fragment.Analytic
import com.iot.android.smartlamp.screen.Fragment.Home
import com.iot.android.smartlamp.screen.Fragment.Setting
import com.iot.android.smartlamp.viewModel.LampVM
import com.iot.android.smartlamp.viewModel.LampVMFactory
import kotlin.getValue

class MainActivity : AppCompatActivity() {
    private lateinit var homeLayout : LinearLayout
    private lateinit var  analyticsLayout : LinearLayout
    private lateinit var settingLayout : LinearLayout
    private lateinit var homeIcon : ImageView
    private lateinit var analyticsIcon : ImageView
    private lateinit var settingIcon : ImageView
    private lateinit var homeLabelText : TextView
    private lateinit var analyticsLabelText : TextView
    private lateinit var settingLabelText : TextView
    private var selectedMenu : Int = 1
    private val lampVM: LampVM by viewModels  {
        // Inject service into ViewModel
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

        homeLayout = findViewById(R.id.menu_home)
        analyticsLayout = findViewById(R.id.menu_analytics)
        settingLayout = findViewById(R.id.menu_setting)
        homeIcon = findViewById(R.id.menu_icon_home)
        analyticsIcon = findViewById(R.id.menu_icon_analytics)
        settingIcon = findViewById(R.id.menu_icon_setting)
        homeLabelText = findViewById(R.id.menu_label_home)
        analyticsLabelText = findViewById(R.id.menu_label_analytics)
        settingLabelText = findViewById(R.id.menu_label_setting)

        homeLayout.setOnClickListener {
            if (selectedMenu == 1) return@setOnClickListener

            supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragment_container, Home()).commit()

            // Make other menu not active
            setMenu(true, homeLayout, homeIcon, homeLabelText)
            setMenu(false, analyticsLayout, analyticsIcon, analyticsLabelText)
            setMenu(false, settingLayout, settingIcon, settingLabelText)
            setMenuAnimation(homeLayout)
            selectedMenu = 1
        }

        analyticsLayout.setOnClickListener {
            if (selectedMenu == 2) return@setOnClickListener

            supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragment_container, Analytic()).commit()

            // Make other menu not active
            setMenu(false, homeLayout, homeIcon, homeLabelText)
            setMenu(true, analyticsLayout, analyticsIcon, analyticsLabelText)
            setMenu(false, settingLayout, settingIcon, settingLabelText)
            setMenuAnimation(analyticsLayout)
            selectedMenu = 2
        }

        settingLayout.setOnClickListener {
            if (selectedMenu == 3) return@setOnClickListener

            supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragment_container, Setting()).commit()

            // Make other menu not active
            setMenu(false, homeLayout, homeIcon, homeLabelText)
            setMenu(false, analyticsLayout, analyticsIcon, analyticsLabelText)
            setMenu(true, settingLayout, settingIcon, settingLabelText)
            setMenuAnimation(settingLayout)
            selectedMenu = 3
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setMenu(isActive : Boolean, menu : LinearLayout, menuIcon : ImageView, menuLabelText : TextView) {
        if (isActive) {
            menu.setBackgroundResource(R.drawable.round_badge)
            menuIcon.setColorFilter(ContextCompat.getColor(this, R.color.senerity_blue))
            menuLabelText.visibility = View.VISIBLE
        } else {
            menu.setBackgroundColor(Color.TRANSPARENT)
            menuIcon.setColorFilter(ContextCompat.getColor(this, R.color.blue_grey))
            menuLabelText.visibility = View.GONE
        }
    }

    private fun setMenuAnimation(menu : LinearLayout) {
        val scaleAnimation = ScaleAnimation(
            0.5f, 1.0f, 1.0f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )

        scaleAnimation.duration = 300
        scaleAnimation.fillAfter = true
        menu.startAnimation(scaleAnimation)
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
        lampVM.scanForDevice { device ->
            lampVM.connectToLamp(device)
        }
    }
}