package com.example.translationapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast

class SpeechRecognition(private val context: Context) {

    private var slanguage: String="en"
    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: ((String) -> Unit)? = null
    private var isListening = false // Track whether listening is active
    private var isPausedForLong = false // Detect long pauses



    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Toast.makeText(context, "Listening...", Toast.LENGTH_SHORT).show()
                        isPausedForLong = false // Reset long pause flag

                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false // Mark end of speech
                        isPausedForLong = true // Mark as paused for a long time
                        stopListeningAfterLongPause()
                    }

                    override fun onError(error: Int) {
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            isPausedForLong = true // Mark as a long pause
                            stopListeningAfterLongPause()
                        } else {
                            Toast.makeText(context, "Error recognizing speech: $error", Toast.LENGTH_SHORT).show()
                            isListening = false
                            restartListening() // Restart for recoverable errors
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false // Stop listening temporarily
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            callback?.invoke(recognizedText) // Send recognized text for translation
                        }
                        restartListening()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val partialText = matches[0]
                            callback?.invoke(partialText) // Send partial results for real-time translation
                        }
                    }


                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            Toast.makeText(context, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun startListening(language: String, callback: (String) -> Unit) {
        this.callback = callback
        slanguage = language
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun restartListening() {
        if (!isListening && !isPausedForLong) {
            speechRecognizer?.cancel() // Cancel current recognizer session
            startListening(slanguage, callback!!)
        }
    }
    private fun stopListeningAfterLongPause() {
        if (isPausedForLong) {
            Toast.makeText(context, "Stopped listening after long pause.", Toast.LENGTH_SHORT).show()
            speechRecognizer?.cancel() // Stop speech recognition
            isListening = false
        }
    }


    // If needed standard recognition (non-continuous)
    fun recognizeSpeech(language: String, callback: (String) -> Unit) {
        this.callback = callback

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        }

        speechRecognizer?.startListening(intent)
    }
    fun destroy() {
        speechRecognizer?.destroy()
    }

}
