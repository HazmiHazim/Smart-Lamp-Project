package com.iot.android.smartlamp.screen

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iot.android.smartlamp.DependencyContainer
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.viewModel.LampVM
import com.iot.android.smartlamp.viewModel.LampVMFactory
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LampDetailActivity : AppCompatActivity() {

    private lateinit var backBtn : ImageView
    private lateinit var lampNameLabelText : TextView
    private lateinit var lampModelLabelText : TextView
    private lateinit var colourAppliedLabelText : TextView
    private lateinit var switch : SwitchMaterial
    private lateinit var switchState : TextView
    private lateinit var brightnessCard : CardView
    private lateinit var brightnessLabelText : TextView
    private lateinit var brightnessSeekBar : SeekBar
    private lateinit var colourPicker : ColorPickerView
    private lateinit var colourAppliedBtn : MaterialButton
    private lateinit var removeBtn : MaterialButton
    private var selectedColourHexCode : String = ""
    private var selectedRedCode : Int = 0
    private var selectedGreenCode : Int = 0
    private var selectedBlueCode : Int = 0
    private var lampStateIsChecked : Boolean = false

    private val lampVM: LampVM by viewModels {
        LampVMFactory(DependencyContainer.provideLampService(this))
    }

    // Lazy-loaded and cached color map
    private var colourMap: Map<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lamp_detail)

        if (savedInstanceState == null) {
            supportActionBar?.hide()
        }

        backBtn = findViewById(R.id.lamp_detail_back_button)
        lampNameLabelText = findViewById(R.id.lamp_detail_label_name)
        lampModelLabelText = findViewById(R.id.lamp_detail_label_model)
        colourAppliedLabelText = findViewById(R.id.lamp_detail_label_colour)
        switch = findViewById(R.id.lamp_switch)
        switchState = findViewById(R.id.switch_state)
        brightnessCard = findViewById(R.id.brightness_card)
        brightnessLabelText = findViewById(R.id.lamp_detail_label_brightness_level)
        brightnessSeekBar = findViewById(R.id.brightness_seekbar)
        colourPicker = findViewById(R.id.lamp_detail_colour_picker)
        colourAppliedBtn = findViewById(R.id.lamp_detail_selected_colour_button)
        removeBtn = findViewById(R.id.lamp_detail_delete_button)

        val lampId = intent.getIntExtra("lamp_id", -1)
        val lampPublicId = intent.getStringExtra("lamp_public_id") ?: ""
        val lampName = intent.getStringExtra("lamp_name") ?: ""
        val lampModel = intent.getStringExtra("lamp_model") ?: ""
        val lampState = intent.getBooleanExtra("lamp_state", false)
        lampStateIsChecked = lampState

        lampNameLabelText.text = lampName
        lampModelLabelText.text = lampModel
        switch.isChecked = lampState
        updateSwitchUI(lampState)

        // Pre-load color map off main thread
        lifecycleScope.launch {
            colourMap = withContext(Dispatchers.IO) {
                loadColourMap(this@LampDetailActivity)
            }
        }

        backBtn.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("updated_lamp_id", lampId)
                putExtra("updated_lamp_state", lampStateIsChecked)
            }
            setResult(Activity.RESULT_FIRST_USER, resultIntent)
            finish()
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            lampStateIsChecked = isChecked
            if (isChecked) {
                lampVM.turnOnCommand(lampPublicId)
                lampVM.toggleLampState(lampId, true)
                updateSwitchUI(true)
                expandCard(brightnessCard)
            } else {
                lampVM.turnOffCommand(lampPublicId)
                lampVM.toggleLampState(lampId, false)
                updateSwitchUI(false)
                collapseCard(brightnessCard)
            }
        }

        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar : SeekBar, progress : Int, fromUser : Boolean) {}
            override fun onStartTrackingTouch(seekBar : SeekBar) {}

            override fun onStopTrackingTouch(seekBar : SeekBar) {
                val finalBrightnessValue = seekBar.progress
                lampVM.setBrightnessCommand(lampPublicId, finalBrightnessValue)
                lampVM.updateBrightness(lampId, finalBrightnessValue)
            }
        })

        colourPicker.setColorListener(ColorEnvelopeListener { envelope, _ ->
            val color = envelope.color
            selectedRedCode = Color.red(color)
            selectedGreenCode = Color.green(color)
            selectedBlueCode = Color.blue(color)
            selectedColourHexCode = String.format("#%06X", 0xFFFFFF and envelope.color)
        })

        colourAppliedBtn.setOnClickListener {
            val colorName = colourMap?.entries
                ?.firstOrNull { it.value.equals(selectedColourHexCode, ignoreCase = true) }
                ?.key
            colourAppliedLabelText.text = colorName ?: selectedColourHexCode
            Toast.makeText(this, "Colour Applied", Toast.LENGTH_LONG).show()
        }

        removeBtn.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmation")
            builder.setMessage("Do you want to proceed with this action?")

            builder.setPositiveButton("Yes") { dialog : DialogInterface, _ : Int ->
                if (lampId != -1) {
                    val resultIntent = Intent().apply {
                        putExtra("deleted_lamp_id", lampId)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                }
                finish()
                dialog.dismiss()
            }

            builder.setNegativeButton("No") { dialog : DialogInterface, _ : Int ->
                dialog.dismiss()
            }

            builder.create().show()
        }
    }

    private fun updateSwitchUI(isOn: Boolean) {
        if (isOn) {
            switchState.setBackgroundResource(R.drawable.custom_switch_label_state_on)
            switchState.text = "ON"
        } else {
            switchState.setBackgroundResource(R.drawable.custom_switch_label_state_off)
            switchState.text = "OFF"
        }
    }

    private fun expandCard(card : CardView) {
        card.measure(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val targetHeight = card.measuredHeight
        card.layoutParams.height = 0
        card.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.addUpdateListener { animation ->
            card.layoutParams.height = animation.animatedValue as Int
            card.requestLayout()
        }

        animator.interpolator = DecelerateInterpolator()
        animator.duration = 300
        animator.start()
    }

    private fun collapseCard(card : CardView) {
        val initialHeight = card.measuredHeight

        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.addUpdateListener { animation ->
            card.layoutParams.height = animation.animatedValue as Int
            card.requestLayout()
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation : Animator) {
                card.visibility = View.GONE
            }
        })

        animator.interpolator = AccelerateInterpolator()
        animator.duration = 300
        animator.start()
    }

    private fun loadColourMap(context: Context): Map<String, String> {
        val inputStream = context.resources.openRawResource(R.raw.colors)
        val json = inputStream.bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(json, type)
    }
}
