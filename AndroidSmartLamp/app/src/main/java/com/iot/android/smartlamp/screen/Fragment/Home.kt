package com.iot.android.smartlamp.screen.Fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.iot.android.smartlamp.DependencyContainer
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.adapter.LampAdapter
import com.iot.android.smartlamp.model.Lamp
import com.iot.android.smartlamp.screen.LampDetailActivity
import com.iot.android.smartlamp.service.api.ApiManager
import com.iot.android.smartlamp.util.ScreenUtils
import com.iot.android.smartlamp.viewModel.LampVM
import com.iot.android.smartlamp.viewModel.LampVMFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
            val popupMenu = PopupMenu(requireContext(), addLampBtn, Gravity.END)
            popupMenu.inflate(R.menu.add_lamp_menu)

            try {
                val field = PopupMenu::class.java.getDeclaredField("mPopup")
                field.isAccessible = true
                val menuHelper = field.get(popupMenu)
                val classPopupHelper = Class.forName(menuHelper.javaClass.name)
                val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                setForceIcons.invoke(menuHelper, true)
            } catch (e : Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.lamp_registration -> {
                        openDialog()
                        true
                    }
                    R.id.lamp_scanner -> {
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }

        swipeRefresh.setOnRefreshListener {
            startDeviceScan()
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

    private fun openDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.lamp_dialog, null)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val lampIdLayout : TextInputLayout = dialogView.findViewById(R.id.home_input_lamp_id_layout)
        val lampIdInput : TextInputEditText = dialogView.findViewById(R.id.home_input_lamp_id)
        val registrationLampBtn : MaterialButton = dialogView.findViewById(R.id.registration_lamp_button)
        val loadingIndicator : CircularProgressIndicator = dialogView.findViewById(R.id.home_dialog_circular_progress)

        fun setLoading(isLoading : Boolean) {
            registrationLampBtn.isEnabled = !isLoading
            registrationLampBtn.text = if (isLoading) "" else "Register Device"
            loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        lampIdInput.addTextChangedListener {
            lampIdLayout.error = null
        }

        registrationLampBtn.setOnClickListener {
            var hasError = false
            val lampId : String = lampIdInput.text.toString()

            if (lampId.isBlank()) {
                lampIdLayout.error = "Lamp ID cannot be empty"
                hasError = true
            } else if (!lampId.isBlank() && lampId.length != 15) {
                lampIdLayout.error = "Lamp ID must be exactly 15 characters"
                hasError = true
            }

            if (hasError) {
                return@setOnClickListener
            }

            setLoading(true)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = ApiManager.api.getLamp(lampId)
                    withContext(Dispatchers.Main) {
                        setLoading(false)

                        if (response.isSuccessful && response.body() != null) {
                            val responseBody = response.body()
                            val rawJson = Gson().toJson(responseBody)
                            val json = JSONObject(rawJson)
                            val lampName = json.optString("name")
                            val lampModel = json.optString("model")
                            val txKey = json.optString("tx_key")
                            val rxKey = json.optString("rx_key")
                            val newLamp = Lamp(publicId = lampId, name = lampName, model = lampModel, state = false, colour = "No Colour Yet", txKey = txKey, rxKey = rxKey)
                            lampVM.addLamp(newLamp)
                            Toast.makeText(requireContext(), "Lamp registered successfully!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(requireContext(), "Lamp not found or invalid ID (${response.code()})", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
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

    private fun startDeviceScan() {
        swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            lampVM.scanForDevice { device ->
                lampVM.connectToLamp(device)
                Toast.makeText(requireContext(), "Scan complete. Found!", Toast.LENGTH_SHORT).show()
            }
            swipeRefresh.isRefreshing = false
        }
    }


}