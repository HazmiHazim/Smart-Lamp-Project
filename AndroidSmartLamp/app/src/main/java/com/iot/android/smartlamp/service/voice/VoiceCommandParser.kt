package com.iot.android.smartlamp.service.voice

import com.iot.android.smartlamp.model.VoiceCommand

object VoiceCommandParser {

    // Common misrecognitions mapped to correct words
    private val wordVariants = mapOf(
        "lamp" to listOf("lamb", "lam", "lem", "lem p", "lump", "lamp p", "lab", "lamb p"),
        "turn" to listOf("then", "tun", "toon", "ten", "tern")
    )

    fun normalizeText(text: String): String {
        var result = text.lowercase().trim()
        for ((correct, variants) in wordVariants) {
            for (variant in variants) {
                result = result.replace(variant, correct)
            }
        }
        return result
    }

    private val lampNumberMap = mapOf(
        "one" to 1, "two" to 2, "three" to 3,
        "won" to 1, "to" to 2, "too" to 2, "tu" to 2, "tue" to 2, "tree" to 3,
        "1" to 1, "2" to 2, "3" to 3,
        // Malay
        "satu" to 1, "dua" to 2, "tiga" to 3
    )

    private val englishOnPattern = Regex("turn on (lamp (\\w+)|all lamps?)")
    private val englishOffPattern = Regex("turn off (lamp (\\w+)|all lamps?)")

    private val malayOnPattern = Regex("(hidupkan|hidup kan) (lampu (\\w+)|semua lampu)")
    private val malayOffPattern = Regex("(padamkan|padam kan) (lampu (\\w+)|semua lampu)")

    fun parseCommand(text: String): VoiceCommand? {
        val normalized = normalizeText(text)

        // English: turn on
        englishOnPattern.find(normalized)?.let { match ->
            if (match.groupValues[1].contains("all")) return VoiceCommand.TurnOnAll
            val lampWord = match.groupValues[2]
            val index = lampNumberMap[lampWord] ?: return null
            return VoiceCommand.TurnOn(index)
        }

        // English: turn off
        englishOffPattern.find(normalized)?.let { match ->
            if (match.groupValues[1].contains("all")) return VoiceCommand.TurnOffAll
            val lampWord = match.groupValues[2]
            val index = lampNumberMap[lampWord] ?: return null
            return VoiceCommand.TurnOff(index)
        }

        // Malay: hidupkan
        malayOnPattern.find(normalized)?.let { match ->
            if (match.groupValues[2].contains("semua")) return VoiceCommand.TurnOnAll
            val lampWord = match.groupValues[3]
            val index = lampNumberMap[lampWord] ?: return null
            return VoiceCommand.TurnOn(index)
        }

        // Malay: padamkan
        malayOffPattern.find(normalized)?.let { match ->
            if (match.groupValues[2].contains("semua")) return VoiceCommand.TurnOffAll
            val lampWord = match.groupValues[3]
            val index = lampNumberMap[lampWord] ?: return null
            return VoiceCommand.TurnOff(index)
        }

        return null
    }

    fun commandToString(command: VoiceCommand): String {
        return when (command) {
            is VoiceCommand.TurnOn -> "Turn on LED ${command.lampIndex}"
            is VoiceCommand.TurnOff -> "Turn off LED ${command.lampIndex}"
            is VoiceCommand.TurnOnAll -> "Turn on all LEDs"
            is VoiceCommand.TurnOffAll -> "Turn off all LEDs"
        }
    }
}
