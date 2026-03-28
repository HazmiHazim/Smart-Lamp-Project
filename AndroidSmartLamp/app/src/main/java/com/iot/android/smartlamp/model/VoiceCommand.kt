package com.iot.android.smartlamp.model

sealed class VoiceCommand {
    data class TurnOn(val lampIndex: Int) : VoiceCommand()
    data class TurnOff(val lampIndex: Int) : VoiceCommand()
    data object TurnOnAll : VoiceCommand()
    data object TurnOffAll : VoiceCommand()
}
