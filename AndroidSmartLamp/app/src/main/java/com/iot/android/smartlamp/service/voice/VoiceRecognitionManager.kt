package com.iot.android.smartlamp.service.voice

import android.content.Context
import android.util.Log
import com.iot.android.smartlamp.model.VoiceCommand
import com.iot.android.smartlamp.service.LampServiceInterface
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.util.concurrent.Executors

class VoiceRecognitionManager(
    private val context: Context,
    private val lampService: LampServiceInterface
) : RecognitionListener {

    enum class State { INITIALIZING, IDLE, LISTENING, PROCESSING, ERROR }

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var currentState = State.INITIALIZING
    val state: State get() = currentState

    var onStateChanged: ((State) -> Unit)? = null
    var onCommandResult: ((String) -> Unit)? = null

    private val executor = Executors.newSingleThreadExecutor()
    private val commandTimeoutRunnable = Runnable { switchToWakeWordMode() }
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    fun initialize() {
        Log.d(TAG, "Initializing voice recognition...")
        setState(State.INITIALIZING)
        StorageService.unpack(context, "model-en-us", "model",
            { model ->
                this.model = model
                Log.d(TAG, "Vosk model loaded successfully")
                switchToWakeWordMode()
            },
            { e ->
                Log.e(TAG, "Failed to load model: ${e.message}", e)
                setState(State.ERROR)
            }
        )
    }

    private fun switchToWakeWordMode() {
        mainHandler.removeCallbacks(commandTimeoutRunnable)
        stopListening()

        val mdl = model ?: return
        val recognizer = Recognizer(mdl, SAMPLE_RATE, "[\"hey lamp\", \"[unk]\"]")

        speechService = SpeechService(recognizer, SAMPLE_RATE)
        speechService?.startListening(this)
        setState(State.IDLE)
        Log.d(TAG, "Wake word mode active")
    }

    private fun switchToCommandMode() {
        stopListening()

        val mdl = model ?: return
        val recognizer = Recognizer(mdl, SAMPLE_RATE)

        speechService = SpeechService(recognizer, SAMPLE_RATE)
        speechService?.startListening(this)
        setState(State.LISTENING)
        Log.d(TAG, "Command mode active")

        mainHandler.postDelayed(commandTimeoutRunnable, COMMAND_TIMEOUT_MS)
    }

    override fun onPartialResult(hypothesis: String?) {
        if (hypothesis == null) return
        val partial = parseJson(hypothesis, "partial")
        if (partial.isBlank()) return

        Log.d(TAG, "Partial: $partial")

        if (currentState == State.IDLE && VoiceCommandParser.normalizeText(partial).contains("hey lamp")) {
            switchToCommandMode()
        }
    }

    override fun onResult(hypothesis: String?) {
        if (hypothesis == null) return
        val text = parseJson(hypothesis, "text")
        if (text.isBlank()) return

        Log.d(TAG, "Result [${currentState}]: $text")

        when (currentState) {
            State.IDLE -> {
                if (VoiceCommandParser.normalizeText(text).contains("hey lamp")) {
                    switchToCommandMode()
                }
            }
            State.LISTENING -> {
                val command = VoiceCommandParser.parseCommand(text)
                if (command != null) {
                    setState(State.PROCESSING)
                    executeCommand(command)
                    val msg = VoiceCommandParser.commandToString(command)
                    mainHandler.post { onCommandResult?.invoke(msg) }
                    mainHandler.postDelayed({ switchToWakeWordMode() }, 1000)
                }
            }
            else -> {}
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        if (hypothesis == null) return
        val text = parseJson(hypothesis, "text")
        Log.d(TAG, "Final: $text")

        if (currentState == State.LISTENING && text.isNotBlank()) {
            val command = VoiceCommandParser.parseCommand(text)
            if (command != null) {
                setState(State.PROCESSING)
                executeCommand(command)
                val msg = VoiceCommandParser.commandToString(command)
                mainHandler.post { onCommandResult?.invoke(msg) }
                mainHandler.postDelayed({ switchToWakeWordMode() }, 1000)
            }
            // If no command matched, keep listening — timeout will handle fallback
        }
    }

    override fun onError(e: Exception?) {
        Log.e(TAG, "Recognition error", e)
        mainHandler.postDelayed({ switchToWakeWordMode() }, 2000)
    }

    override fun onTimeout() {
        Log.d(TAG, "Timeout")
        switchToWakeWordMode()
    }

    private fun executeCommand(command: VoiceCommand) {
        executor.execute {
            val lamps = lampService.getAllLamps() ?: return@execute

            when (command) {
                is VoiceCommand.TurnOn -> {
                    val lamp = lamps.getOrNull(command.lampIndex - 1) ?: return@execute
                    lampService.turnOnCommand(lamp.publicId)
                    lampService.updateLampState(lamp.id, true)
                }
                is VoiceCommand.TurnOff -> {
                    val lamp = lamps.getOrNull(command.lampIndex - 1) ?: return@execute
                    lampService.turnOffCommand(lamp.publicId)
                    lampService.updateLampState(lamp.id, false)
                }
                is VoiceCommand.TurnOnAll -> {
                    for (lamp in lamps) {
                        lampService.turnOnCommand(lamp.publicId)
                        lampService.updateLampState(lamp.id, true)
                    }
                }
                is VoiceCommand.TurnOffAll -> {
                    for (lamp in lamps) {
                        lampService.turnOffCommand(lamp.publicId)
                        lampService.updateLampState(lamp.id, false)
                    }
                }
            }
        }
    }

    private fun stopListening() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    fun stop() {
        mainHandler.removeCallbacks(commandTimeoutRunnable)
        stopListening()
        model?.close()
        model = null
        setState(State.INITIALIZING)
    }

    private fun setState(state: State) {
        currentState = state
        mainHandler.post { onStateChanged?.invoke(state) }
    }

    private fun parseJson(json: String, key: String): String {
        return try {
            JSONObject(json).optString(key, "")
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        private const val TAG = "VoiceRecognition"
        private const val SAMPLE_RATE = 16000.0f
        private const val COMMAND_TIMEOUT_MS = 8000L
    }
}
