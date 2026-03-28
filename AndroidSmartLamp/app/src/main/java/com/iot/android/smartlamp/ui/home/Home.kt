package com.iot.android.smartlamp.ui.home

import android.Manifest
import android.app.Activity
import android.util.Log
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.di.DependencyContainer
import com.iot.android.smartlamp.model.Lamp
import com.iot.android.smartlamp.service.voice.VoiceRecognitionManager
import com.iot.android.smartlamp.ui.adapter.LampAdapter
import com.iot.android.smartlamp.ui.detail.LampDetailActivity
import com.iot.android.smartlamp.ui.LampVM
import com.iot.android.smartlamp.ui.LampVMFactory
import com.iot.android.smartlamp.util.ScreenUtils

class Home : Fragment(R.layout.home) {

    private lateinit var topBar : LinearLayout
    private lateinit var voiceStatusBar : LinearLayout
    private lateinit var voiceStatusText : TextView
    private lateinit var shimmerLayout : ShimmerFrameLayout
    private lateinit var swipeRefresh : SwipeRefreshLayout
    private lateinit var recyclerView : RecyclerView
    private lateinit var emptyCard : CardView
    private lateinit var emptyCardBulb : ImageView
    private lateinit var emptyCardText : TextView
    private lateinit var lampAdapter : LampAdapter
    private val lampVM: LampVM by activityViewModels {
        LampVMFactory(DependencyContainer.provideLampService(requireActivity()))
    }

    private val lampDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val deletedId = result.data?.getIntExtra("deleted_lamp_id", -1) ?: -1
                if (deletedId != -1) lampVM.deleteLamp(deletedId)
            }
            Activity.RESULT_FIRST_USER -> {
                val updatedId = result.data?.getIntExtra("updated_lamp_id", -1) ?: -1
                val updatedState = result.data?.getBooleanExtra("updated_lamp_state", false) ?: false
                val updatedColour = result.data?.getStringExtra("updated_lamp_colour") ?: ""
                if (updatedId != -1) {
                    lampVM.toggleLampState(updatedId, updatedState)
                    if (updatedColour.isNotEmpty()) lampVM.updateLampColour(updatedId, updatedColour)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        topBar = view.findViewById(R.id.topbar)
        voiceStatusBar = view.findViewById(R.id.voice_status_bar)
        voiceStatusText = view.findViewById(R.id.voice_status_text)
        shimmerLayout = view.findViewById(R.id.home_shimmer_layout)
        swipeRefresh = view.findViewById(R.id.home_swipe_refresh)
        recyclerView = view.findViewById(R.id.home_recyclerview)
        emptyCard = view.findViewById(R.id.home_lamp_empty_card)
        emptyCardBulb = view.findViewById(R.id.home_lamp_empty_card_bulb)
        emptyCardText = view.findViewById(R.id.home_lamp_empty_card_text)
        lampAdapter = LampAdapter(emptyList()) { lamp -> openLampDetail(lamp) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = lampAdapter

        ScreenUtils.resizeImage(emptyCardBulb, 100)

        swipeRefresh.setOnRefreshListener {
            if (hasBluetoothPermission()) {
                Toast.makeText(requireContext(), "Scanning for devices...", Toast.LENGTH_SHORT).show()
                scanForDevices()
            } else {
                Toast.makeText(requireContext(), "Bluetooth permission required", Toast.LENGTH_SHORT).show()
            }
            swipeRefresh.isRefreshing = false
        }

        val hasExistingData = lampVM.lampList.value?.isNotEmpty() == true

        if (hasExistingData) {
            shimmerLayout.visibility = View.GONE
            swipeRefresh.visibility = View.VISIBLE
        } else if (hasBluetoothPermission()) {
            scanForDevices()
        } else {
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            swipeRefresh.visibility = View.VISIBLE
            emptyCardText.setText(R.string.no_device_found)
        }

        lampVM.lampList.observe(viewLifecycleOwner) { list ->
            lampAdapter.updateList(list)
            updateLampCardState(list)
        }

        setupVoiceStatus()
    }

    private fun setupVoiceStatus() {
        val voiceManager = DependencyContainer.provideVoiceRecognitionManager(requireActivity())
        Log.d("VoiceUI", "setupVoiceStatus called, current state: ${voiceManager.state}")

        voiceManager.onStateChanged = { state ->
            Log.d("VoiceUI", "State changed to: $state")
            updateVoiceUI(state)
        }

        voiceManager.onCommandResult = { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        // Apply current state immediately in case init already completed
        Log.d("VoiceUI", "Applying initial state: ${voiceManager.state}")
        updateVoiceUI(voiceManager.state)
    }

    private fun updateVoiceUI(state: VoiceRecognitionManager.State) {
        Log.d("VoiceUI", "updateVoiceUI: $state, bar visible: ${voiceStatusBar.visibility}")
        when (state) {
            VoiceRecognitionManager.State.INITIALIZING -> {
                voiceStatusBar.visibility = View.VISIBLE
                voiceStatusText.setText(R.string.voice_initializing)
            }
            VoiceRecognitionManager.State.IDLE -> {
                voiceStatusBar.visibility = View.VISIBLE
                voiceStatusText.setText(R.string.voice_idle)
            }
            VoiceRecognitionManager.State.LISTENING -> {
                voiceStatusBar.visibility = View.VISIBLE
                voiceStatusText.setText(R.string.voice_listening)
            }
            VoiceRecognitionManager.State.PROCESSING -> {
                voiceStatusBar.visibility = View.VISIBLE
                voiceStatusText.text = "Processing..."
            }
            VoiceRecognitionManager.State.ERROR -> {
                voiceStatusBar.visibility = View.GONE
            }
        }
    }

    private fun updateLampCardState(list : List<Lamp>) {
        if (list.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyCard.visibility = View.VISIBLE
        } else {
            emptyCard.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionGranted() {
        if (lampVM.lampList.value.isNullOrEmpty()) {
            shimmerLayout.visibility = View.VISIBLE
            shimmerLayout.startShimmer()
            swipeRefresh.visibility = View.GONE
            scanForDevices()
        }
    }

    private var deviceFound = false

    private fun scanForDevices() {
        deviceFound = false
        lampVM.scanForDevice(
            onFound = { device ->
                lampVM.connectToLamp(device)
                if (!deviceFound) {
                    deviceFound = true
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Device found", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onScanComplete = {
                requireActivity().runOnUiThread {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    swipeRefresh.visibility = View.VISIBLE
                    if (!deviceFound) {
                        emptyCardText.setText(R.string.no_device_found)
                        Toast.makeText(requireContext(), "No device found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun openLampDetail(lamp : Lamp) {
        val intent = Intent(requireContext(), LampDetailActivity::class.java).apply {
            putExtra("lamp_id", lamp.id)
            putExtra("lamp_public_id", lamp.publicId)
            putExtra("lamp_name", lamp.name)
            putExtra("lamp_state", lamp.state)
            putExtra("lamp_colour", lamp.colour)
        }
        lampDetailLauncher.launch(intent)
    }
}
