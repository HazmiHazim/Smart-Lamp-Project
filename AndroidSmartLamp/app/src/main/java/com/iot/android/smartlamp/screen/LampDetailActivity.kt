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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iot.android.smartlamp.DependencyContainer
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.viewModel.LampVM
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlin.getValue

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
    private val lampVM: LampVM by lazy {
        // Inject service into ViewModel
        LampVM(lampService = DependencyContainer.provideLampService(this))
    }

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

        val LAMP_ID = intent.getIntExtra("lamp_id", -1)
        val LAMP_PUBLIC_ID = intent.getStringExtra("lamp_public_id").toString()
        val LAMP_NAME = intent.getStringExtra("lamp_name").toString()
        val LAMP_MODEL = intent.getStringExtra("lamp_model").toString()
        val LAMP_STATE = intent.getBooleanExtra("lamp_state", false)

        lampNameLabelText.text = LAMP_NAME
        lampModelLabelText.text = LAMP_MODEL
        switch.isChecked = LAMP_STATE
        if (LAMP_STATE) {
            switchState.setBackgroundResource(R.drawable.custom_switch_label_state_on)
            switchState.text = "ON"
        } else {
            switchState.setBackgroundResource(R.drawable.custom_switch_label_state_off)
            switchState.text = "OFF"
        }

        backBtn.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("updated_lamp_id", LAMP_ID)
                putExtra("updated_lamp_state", lampStateIsChecked)
            }
            setResult(Activity.RESULT_FIRST_USER, resultIntent)
            finish()
        }

        switch.setOnCheckedChangeListener { button, isChecked ->
            if (isChecked) {
                try {
                    lampStateIsChecked = true
                    lampVM.turnOnCommand(LAMP_PUBLIC_ID)
                    lampVM.toggleLampState(LAMP_ID, lampStateIsChecked)
                    switchState.setBackgroundResource(R.drawable.custom_switch_label_state_on)
                    switchState.text = "ON"
                    expandCard(brightnessCard)
                } catch (e : Exception) {
                    lampStateIsChecked = false
                    lampVM.turnOffCommand(LAMP_PUBLIC_ID)
                    lampVM.toggleLampState(LAMP_ID, lampStateIsChecked)
                    switchState.setBackgroundResource(R.drawable.custom_switch_label_state_off)
                    switchState.text = "OFF"
                    collapseCard(brightnessCard)
                    Toast.makeText(this, "Error occur: " + e.message,
                        Toast.LENGTH_SHORT).show()
                }
            } else {
                try {
                    lampStateIsChecked = false
                    lampVM.turnOffCommand(LAMP_PUBLIC_ID)
                    lampVM.toggleLampState(LAMP_ID, lampStateIsChecked)
                    switchState.setBackgroundResource(R.drawable.custom_switch_label_state_off)
                    switchState.text = "OFF"
                    collapseCard(brightnessCard)
                    Toast.makeText(this, "Successful turn off Lamp",
                        Toast.LENGTH_SHORT).show()
                } catch (e : Exception) {
                    lampStateIsChecked = false
                    lampVM.turnOffCommand(LAMP_PUBLIC_ID)
                    lampVM.toggleLampState(LAMP_ID, lampStateIsChecked)
                    switchState.setBackgroundResource(R.drawable.custom_switch_label_state_off)
                    switchState.text = "OFF"
                    collapseCard(brightnessCard)
                    Toast.makeText(this, "Error occur: " + e.message,
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar : SeekBar, progress : Int, fromUser : Boolean) {
                //TODO("Not yet implemented")
            }

            override fun onStartTrackingTouch(seekBar : SeekBar) {
                //TODO("Not yet implemented")
            }

            override fun onStopTrackingTouch(seekBar : SeekBar) {
                val finalBrightnessValue = seekBar.progress

                try {
                    //TODO("DB PROCESS")
                } catch (e : Exception) {
                    //TODO("DB PROCESS")
                } finally {
                    //TODO("DB PROCESS")
                }
            }

        })

        colourPicker.setColorListener(ColorEnvelopeListener { envelope, bool ->
            val color = envelope.color
            selectedRedCode = Color.red(color)
            selectedGreenCode = Color.green(color)
            selectedBlueCode = Color.blue(color)
            selectedColourHexCode = String.format("#%06X", 0xFFFFFF and envelope.color)
        })

        colourAppliedBtn.setOnClickListener {
            val colorName = getColourByHexCode(this, selectedColourHexCode)
            colourAppliedLabelText.text = colorName ?: selectedColourHexCode
            Toast.makeText(this, "Colour Applied", Toast.LENGTH_LONG).show()
        }

        removeBtn.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmation")
            builder.setMessage("Do you want to proceed with this action?")

            builder.setPositiveButton("Yes") { dialog : DialogInterface, which : Int ->
                val lampId = intent.getIntExtra("lamp_id", -1)
                if (lampId != -1) {
                    val resultIntent = Intent().apply {
                        putExtra("deleted_lamp_id", lampId)
                    }
                    setResult(Activity.RESULT_OK, resultIntent) // send result back
                }

                finish()
                dialog.dismiss()
            }

            builder.setNegativeButton("No") { dialog : DialogInterface, which : Int ->
                dialog.dismiss()
            }

            builder.create().show()
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

    private fun getColourByHexCode(context : Context, hexCode : String): String? {
        val inputStream = context.resources.openRawResource(R.raw.colors)
        val json = inputStream.bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, String>>() {}.type
        val colourMap : Map<String, String> = Gson().fromJson(json, type)
        return colourMap.entries.firstOrNull { it.value.equals(hexCode, ignoreCase = true) }?.key
    }

}