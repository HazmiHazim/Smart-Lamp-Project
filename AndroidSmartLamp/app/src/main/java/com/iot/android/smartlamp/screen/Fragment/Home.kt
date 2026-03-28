package com.iot.android.smartlamp.screen.Fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.iot.android.smartlamp.DependencyContainer
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.adapter.LampAdapter
import com.iot.android.smartlamp.model.Lamp
import com.iot.android.smartlamp.screen.LampDetailActivity
import com.iot.android.smartlamp.util.ScreenUtils
import com.iot.android.smartlamp.viewModel.LampVM
import com.iot.android.smartlamp.viewModel.LampVMFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Home : Fragment(R.layout.home) {

    private lateinit var topBar : LinearLayout
    private lateinit var addLampBtn : ImageView
    private lateinit var shimmerLayout : ShimmerFrameLayout
    private lateinit var swipeRefresh : SwipeRefreshLayout
    private lateinit var recyclerView : RecyclerView
    private lateinit var emptyCard : CardView
    private lateinit var emptyCardBulb : ImageView
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
                if (updatedId != -1) lampVM.toggleLampState(updatedId, updatedState)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        topBar = view.findViewById(R.id.topbar)
        addLampBtn = view.findViewById(R.id.add_lamp_button)
        swipeRefresh = view.findViewById(R.id.home_swipe_refresh)
        shimmerLayout = view.findViewById(R.id.home_shimmer_layout)
        recyclerView = view.findViewById(R.id.home_recyclerview)
        emptyCard = view.findViewById(R.id.home_lamp_empty_card)
        emptyCardBulb = view.findViewById(R.id.home_lamp_empty_card_bulb)
        lampAdapter = LampAdapter(mutableListOf()) { lamp -> openLampDetail(lamp) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = lampAdapter

        ScreenUtils.resizeImage(emptyCardBulb, 100)

        addLampBtn.setOnClickListener {
            scanAndAddLamps()
        }

        swipeRefresh.setOnRefreshListener {
            scanAndAddLamps()
        }

        lampVM.lampList.observe(viewLifecycleOwner) { list ->
            shimmerLayout.startShimmer()
            swipeRefresh.visibility = View.GONE
            viewLifecycleOwner.lifecycleScope.launch {
                delay(3000)
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                swipeRefresh.visibility = View.VISIBLE
                lampAdapter.updateList(list)
                updateLampCardState(list)
            }
        }
    }

    private fun updateLampCardState(list : List<Lamp>) {
        if (list.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyCard.visibility = View.VISIBLE
            swipeRefresh.isEnabled = false
        } else {
            emptyCard.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            swipeRefresh.isEnabled = true
        }
    }

    private fun scanAndAddLamps() {
        Toast.makeText(requireContext(), "Scanning for ESP32 devices...", Toast.LENGTH_SHORT).show()
        lampVM.scanForDevice { device ->
            lampVM.connectToLamp(device)
        }
        swipeRefresh.isRefreshing = false
    }

    private fun openLampDetail(lamp : Lamp) {
        val intent = Intent(requireContext(), LampDetailActivity::class.java).apply {
            putExtra("lamp_id", lamp.id)
            putExtra("lamp_public_id", lamp.publicId)
            putExtra("lamp_name", lamp.name)
            putExtra("lamp_model", lamp.model)
            putExtra("lamp_state", lamp.state)
            putExtra("lamp_colour", lamp.colour)
            putExtra("lamp_brightness", lamp.brightness)
        }
        lampDetailLauncher.launch(intent)
    }
}
