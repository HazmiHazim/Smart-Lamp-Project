package com.iot.android.smartlamp.ui.detail

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.di.DependencyContainer
import com.iot.android.smartlamp.service.LampServiceInterface
import com.iot.android.smartlamp.util.ColorUtils
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LampDetailActivity : AppCompatActivity() {

    private lateinit var backBtn : ImageView
    private lateinit var lampNameLabelText : TextView
    private lateinit var switch : SwitchMaterial
    private lateinit var switchState : TextView
    private lateinit var brightnessCard : CardView
    private lateinit var colourCard : CardView
    private lateinit var brightnessLabelText : TextView
    private lateinit var brightnessSeekBar : SeekBar
    private lateinit var colourPicker : ColorPickerView
    private lateinit var colourAppliedBtn : MaterialButton
    private lateinit var removeBtn : MaterialButton
    private var selectedRedCode : Int = 0
    private var selectedGreenCode : Int = 0
    private var selectedBlueCode : Int = 0
    private var lampStateIsChecked : Boolean = false
    private var currentColourName : String = "White"

    private lateinit var lampService: LampServiceInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.lamp_detail)

        lampService = DependencyContainer.provideLampService(this)

        backBtn = findViewById(R.id.lamp_detail_back_button)
        lampNameLabelText = findViewById(R.id.lamp_detail_label_name)
        switch = findViewById(R.id.lamp_switch)
        switchState = findViewById(R.id.switch_state)
        brightnessCard = findViewById(R.id.brightness_card)
        colourCard = findViewById(R.id.colour_card)
        brightnessLabelText = findViewById(R.id.lamp_detail_label_brightness_level)
        brightnessSeekBar = findViewById(R.id.brightness_seekbar)
        colourPicker = findViewById(R.id.lamp_detail_colour_picker)
        colourAppliedBtn = findViewById(R.id.lamp_detail_selected_colour_button)
        removeBtn = findViewById(R.id.lamp_detail_delete_button)

        ColorUtils.loadColors(this)

        val lampId = intent.getIntExtra("lamp_id", -1)
        val lampPublicId = intent.getStringExtra("lamp_public_id") ?: ""
        val lampName = intent.getStringExtra("lamp_name") ?: ""
        val lampState = intent.getBooleanExtra("lamp_state", false)
        currentColourName = intent.getStringExtra("lamp_colour") ?: "White"
        lampStateIsChecked = lampState

        lampNameLabelText.text = lampName
        switch.isChecked = lampState
        updateSwitchUI(lampState)

        if (lampState) {
            brightnessCard.visibility = View.VISIBLE
            colourCard.visibility = View.VISIBLE
            brightnessSeekBar.progress = 255
            brightnessLabelText.text = "100%"
        }

        backBtn.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("updated_lamp_id", lampId)
                putExtra("updated_lamp_state", lampStateIsChecked)
                putExtra("updated_lamp_colour", currentColourName)
            }
            setResult(Activity.RESULT_FIRST_USER, resultIntent)
            finish()
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            lampStateIsChecked = isChecked
            if (isChecked) {
                val (r, g, b) = ColorUtils.getRgbFromColorName(currentColourName)
                lampService.setColorCommand(lampPublicId, r, g, b)
                updateSwitchUI(true)
                brightnessSeekBar.progress = 255
                brightnessLabelText.text = "100%"
                expandCard(brightnessCard)
                expandCard(colourCard)
            } else {
                lampService.turnOffCommand(lampPublicId)
                updateSwitchUI(false)
                brightnessSeekBar.progress = 0
                brightnessLabelText.text = "0%"
                collapseCard(brightnessCard)
                collapseCard(colourCard)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                lampService.updateLampState(lampId, isChecked)
            }
        }

        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar : SeekBar, progress : Int, fromUser : Boolean) {
                val percent = (progress * 100) / 255
                brightnessLabelText.text = "$percent%"
            }
            override fun onStartTrackingTouch(seekBar : SeekBar) {}

            override fun onStopTrackingTouch(seekBar : SeekBar) {
                val finalBrightnessValue = seekBar.progress
                lampService.setBrightnessCommand(lampPublicId, finalBrightnessValue)
            }
        })

        colourPicker.setColorListener(ColorEnvelopeListener { envelope, _ ->
            val color = envelope.color
            selectedRedCode = Color.red(color)
            selectedGreenCode = Color.green(color)
            selectedBlueCode = Color.blue(color)
        })

        val (initR, initG, initB) = ColorUtils.getRgbFromColorName(currentColourName)
        selectedRedCode = initR
        selectedGreenCode = initG
        selectedBlueCode = initB
        colourPicker.setInitialColor(Color.rgb(initR, initG, initB))

        colourAppliedBtn.setOnClickListener {
            lampService.setColorCommand(lampPublicId, selectedRedCode, selectedGreenCode, selectedBlueCode)
            currentColourName = ColorUtils.findClosestColorName(selectedRedCode, selectedGreenCode, selectedBlueCode)
            lifecycleScope.launch(Dispatchers.IO) {
                lampService.updateLampColour(lampId, currentColourName)
            }
            Toast.makeText(this, "Colour Applied: $currentColourName", Toast.LENGTH_LONG).show()
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
}
