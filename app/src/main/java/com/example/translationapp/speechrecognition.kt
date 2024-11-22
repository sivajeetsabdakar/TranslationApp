package com.example.translationapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast

class SpeechRecognition(private val context: Context) {

    private var language: String="en"
    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: ((String) -> Unit)? = null
    private var isListening = false // Track whether listening is active


    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Toast.makeText(context, "Listening...", Toast.LENGTH_SHORT).show()
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false // Mark end of speech
                    }

                    override fun onError(error: Int) {
                        Toast.makeText(context, "Error recognizing speech: $error", Toast.LENGTH_SHORT).show()
                        isListening = false // Reset listening state
                        restartListening(language) // Pass the language parameter
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false // Stop listening temporarily
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            callback?.invoke(recognizedText) // Send recognized text for translation
                        }
                        restartListening(language) // Pass the language parameter
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

    // Starts continuous recognition, sends partial results to callback as you speak
    fun startListening(language: String, callback: (String) -> Unit) {
        this.callback = callback
        this.language = language
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun restartListening(language: String) {
        if (!isListening) {
            speechRecognizer?.cancel() // Cancel current recognizer session
            startListening(language, callback!!)
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
