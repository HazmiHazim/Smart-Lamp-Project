package com.iot.android.smartlamp.screen.Fragment

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.iot.android.smartlamp.R

class Setting : Fragment(R.layout.setting) {

    private lateinit var avatarAnim : LottieAnimationView
    private lateinit var totalDeviceText : TextView
    private lateinit var voiceAssistanceMenu : LinearLayout
    private lateinit var helpFeedbackMenu : LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        avatarAnim = view.findViewById(R.id.avatar_animation)
        totalDeviceText = view.findViewById(R.id.setting_label_total_device)
        voiceAssistanceMenu = view.findViewById(R.id.setting_submenu_voice_assistant)
        helpFeedbackMenu = view.findViewById(R.id.setting_submenu_help_feedback)
    }
}