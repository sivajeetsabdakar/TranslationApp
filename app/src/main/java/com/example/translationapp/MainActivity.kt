package com.example.translationapp

import Translate
//import TranslatorManager
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognition: SpeechRecognition
    private lateinit var translate: Translate
//    private lateinit var translatorManager: TranslatorManager
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var leftTextBox: TextView
    private lateinit var rightTextBox: TextView
    private lateinit var leftLanguageDropdown: Spinner
    private lateinit var rightLanguageDropdown: Spinner
    private lateinit var leftEditText: EditText
    private lateinit var rightEditText: EditText
    private lateinit var leftSendButton: Button
    private lateinit var rightSendButton: Button
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var textToSpeech: TextToSpeech

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Translate, SpeechRecognition, and TranslatorManager
        translate = Translate()
        speechRecognition = SpeechRecognition(this)
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.ENGLISH // Default language
            } else {
                Toast.makeText(this, "TTS Initialization failed!", Toast.LENGTH_SHORT).show()
            }
        }
//        translatorManager = TranslatorManager(speechRecognition, textToSpeech, translate)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.ENGLISH // Default language
            }
        }
        audioPlayer = AudioPlayer(this, textToSpeech)

        // Find UI elements by ID
        leftButton = findViewById(R.id.leftButton)
        rightButton = findViewById(R.id.rightButton)
        leftTextBox = findViewById(R.id.leftTextBox)
        rightTextBox = findViewById(R.id.rightTextBox)
        leftLanguageDropdown = findViewById(R.id.leftLanguageDropdown)
        rightLanguageDropdown = findViewById(R.id.rightLanguageDropdown)
        leftEditText = findViewById(R.id.leftEditText)
        rightEditText = findViewById(R.id.rightEditText)
        leftSendButton = findViewById(R.id.leftSendButton)
        rightSendButton = findViewById(R.id.rightSendButton)

        // Handle Send button clicks for text input
        leftSendButton.setOnClickListener {
            handleTextInput(isLeft = true)
        }

        rightSendButton.setOnClickListener {
            handleTextInput(isLeft = false)
        }

        // Set up language dropdowns
        leftLanguageDropdown.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            getLanguagesArray()
        )

        rightLanguageDropdown.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            getLanguagesArray()
        )

        // Check if RECORD_AUDIO permission is granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        }

        leftButton.setOnClickListener {
            handleButtonClick(isLeft = true)
        }

        rightButton.setOnClickListener {
            handleButtonClick(isLeft = false)
        }
    }

//    private fun checkPermissions() {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
//        }
//    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission required to use speech recognition", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleTextInput(isLeft: Boolean) {
        val inputText = if (isLeft) leftEditText.text.toString().trim() else rightEditText.text.toString().trim()
        val sourceLanguage = if (isLeft) leftLanguageDropdown.selectedItem.toString() else rightLanguageDropdown.selectedItem.toString()
        val targetLanguage = if (isLeft) rightLanguageDropdown.selectedItem.toString() else leftLanguageDropdown.selectedItem.toString()

        if (inputText.isNotEmpty()) {
            val translator = Translate()
            translator.translateText(inputText, sourceLanguage, targetLanguage) { translatedText ->
                runOnUiThread {
                    if (isLeft) {
                        rightEditText.setText(translatedText)
                    } else {
                        leftEditText.setText(translatedText)
                    }
                }
                val locale = Locale.forLanguageTag(getLanguageCode(targetLanguage))
                audioPlayer.speakThroughEarphone(translatedText, isLeft, locale)
            }
        } else {
            Toast.makeText(this, "Please enter text to translate.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun handleButtonClick(isLeft: Boolean) {
        val sourceLanguage = if (isLeft) leftLanguageDropdown.selectedItem.toString() else rightLanguageDropdown.selectedItem.toString()
        val targetLanguage = if (isLeft) rightLanguageDropdown.selectedItem.toString() else leftLanguageDropdown.selectedItem.toString()

        val sourceLanguageCode = getLanguageCode(sourceLanguage)
        val targetLanguageCode = getLanguageCode(targetLanguage)

        // Start continuous speech recognition
        speechRecognition.recognizeSpeech(sourceLanguageCode) { recognizedText ->
            if (isLeft) {
                leftTextBox.text = recognizedText
            } else {
                rightTextBox.text = recognizedText
            }
            translate.translateText(recognizedText, sourceLanguage, targetLanguage) { translatedText ->
                runOnUiThread {
                    if (isLeft) {
                        rightTextBox.text = translatedText
                    } else {
                        leftTextBox.text = translatedText
                    }

                    val locale = Locale.forLanguageTag(getLanguageCode(targetLanguage))
                    audioPlayer.speakThroughEarphone(translatedText, isLeft, locale)
                }
            }
        }
    }
    private fun getLanguageCode(language: String): String {
        return when (language.lowercase()) {
            "english" -> "en"
            "spanish" -> "es"
            "french" -> "fr"
            "german" -> "de"
            "hindi" -> "hi"
            "arabic" -> "ar"
            "bengali" -> "bn"
            "chinese" -> "zh"
            "dutch" -> "nl"
            "italian" -> "it"
            "japanese" -> "ja"
            "korean" -> "ko"
            "malay" -> "ms"
            "portuguese" -> "pt"
            "russian" -> "ru"
            "turkish" -> "tr"
            "vietnamese" -> "vi"
            "thai" -> "th"
            "filipino" -> "tl"
            "swedish" -> "sv"
            "norwegian" -> "no"
            "danish" -> "da"
            "finnish" -> "fi"
            "hebrew" -> "iw"
            "swahili" -> "sw"
            "ukrainian" -> "uk"
            "czech" -> "cs"
            "hungarian" -> "hu"
            "romanian" -> "ro"
            "slovak" -> "sk"
            "bulgarian" -> "bg"
            "croatian" -> "hr"
            "serbian" -> "sr"
            "slovenian" -> "sl"
            "lithuanian" -> "lt"
            "latvian" -> "lv"
            "estonian" -> "et"
            "persian" -> "fa"
            "telugu" -> "te"
            "tamil" -> "ta"
            "marathi" -> "mr"
            "kannada" -> "kn"
            "punjabi" -> "pa"
            "gujarati" -> "gu"
            "burmese" -> "my"
            "armenian" -> "hy"
            "georgian" -> "ka"
            "khmer" -> "km"
            "lao" -> "lo"
            "malagasy" -> "mg"
            "sinhala" -> "si"
            "tigrinya" -> "ti"
            "yiddish" -> "yi"
            else -> "en"  // Default to English if not found
        }
    }
    private fun getLanguagesArray(): Array<String> {
        return arrayOf(
            "English",
            "Arabic",
            "Azerbaijani",
            "Bulgarian",
            "Bengali",
            "Catalan",
            "Czech",
            "Danish",
            "German",
            "Greek",
            "Esperanto",
            "Spanish",
            "Estonian",
            "Persian (Farsi)",
            "Finnish",
            "French",
            "Irish",
            "Hebrew",
            "Hindi",
            "Hungarian",
            "Indonesian",
            "Italian",
            "Japanese",
            "Korean",
            "Lithuanian",
            "Latvian",
            "Malay",
            "Norwegian Bokmål",
            "Dutch",
            "Polish",
            "Portuguese",
            "Romanian",
            "Russian",
            "Slovak",
            "Slovenian",
            "Albanian",
            "Swedish",
            "Thai",
            "Filipino (Tagalog)",
            "Turkish",
            "Ukrainian",
            "Urdu",
            "Chinese",
            "Chinese (Traditional)"
        )
    }
    override fun onDestroy() {
        // Shutdown TTS to free up resources
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}
