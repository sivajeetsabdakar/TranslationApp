package com.example.translationapp

import android.content.Context
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import java.io.File
import java.util.Locale

class TextToSpeechManager(private val context: Context) : OnInitListener {

    private val textToSpeech: TextToSpeech = TextToSpeech(context, this)

    init {
        // Set up the TextToSpeech object
        textToSpeech.language = Locale.getDefault()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
        } else {
            // Handle initialization failure
        }
    }

    fun speak(text: String) {
        // Convert text to speech
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        // Shutdown the TextToSpeech engine
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    fun setLanguage(language: String) {
        val locale = when (language.lowercase()) {
            "english" -> Locale("en")
            "spanish" -> Locale("es")
            "french" -> Locale("fr")
            "german" -> Locale("de")
            "hindi" -> Locale("hi")
            "arabic" -> Locale("ar")
            "bengali" -> Locale("bn")
            "chinese" -> Locale("zh")
            "dutch" -> Locale("nl")
            "italian" -> Locale("it")
            "japanese" -> Locale("ja")
            "korean" -> Locale("ko")
            "malay" -> Locale("ms")
            "portuguese" -> Locale("pt")
            "russian" -> Locale("ru")
            "turkish" -> Locale("tr")
            "vietnamese" -> Locale("vi")
            "thai" -> Locale("th")
            "filipino" -> Locale("tl")
            "swedish" -> Locale("sv")
            "norwegian" -> Locale("no")
            "danish" -> Locale("da")
            "finnish" -> Locale("fi")
            "hebrew" -> Locale("iw")
            "swahili" -> Locale("sw")
            "ukrainian" -> Locale("uk")
            "czech" -> Locale("cs")
            "hungarian" -> Locale("hu")
            "romanian" -> Locale("ro")
            "slovak" -> Locale("sk")
            "bulgarian" -> Locale("bg")
            "croatian" -> Locale("hr")
            "serbian" -> Locale("sr")
            "slovenian" -> Locale("sl")
            "lithuanian" -> Locale("lt")
            "latvian" -> Locale("lv")
            "estonian" -> Locale("et")
            "persian" -> Locale("fa")
            "telugu" -> Locale("te")
            "tamil" -> Locale("ta")
            "marathi" -> Locale("mr")
            "kannada" -> Locale("kn")
            "punjabi" -> Locale("pa")
            "gujarati" -> Locale("gu")
            "burmese" -> Locale("my")
            "armenian" -> Locale("hy")
            "georgian" -> Locale("ka")
            "khmer" -> Locale("km")
            "lao" -> Locale("lo")
            "malagasy" -> Locale("mg")
            "sinhala" -> Locale("si")
            "tigrinya" -> Locale("ti")
            "yiddish" -> Locale("yi")
            else -> Locale("en") // Default to English
        }

        val result = textToSpeech.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            println("Language not supported: $locale")
        }
    }


    fun saveTextToSpeechToFile(text: String) {
        val internalFilesDir = context.filesDir

        // Specify the file where the TTS output will be saved
        val file = File(internalFilesDir, "tts_output.wav")

        // Ensure the parent directory exists
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs() // Create the directory
        }

        // Create a Bundle for parameters
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tts_output")
        }

        try {
            // Synthesize the speech and save it to a file
            val result = textToSpeech.synthesizeToFile(
                text,
                params,
                file,
                "tts_output"
            )

            // Check the result of the synthesis
            if (result == TextToSpeech.SUCCESS) {
                // Successfully saved
                println("TTS audio saved to ${file.absolutePath}")
            } else {
                // Handle failure
                println("Failed to save TTS audio")
            }
        } catch (e: Exception) {
            e.printStackTrace() // Print stack trace for debugging
        }

    }




}
