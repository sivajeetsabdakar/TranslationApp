package com.example.translationapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognition: SpeechRecognition
    private lateinit var translate: Translate
    private lateinit var textToSpeechManager: TextToSpeechManager
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var leftTextBox: TextView
    private lateinit var rightTextBox: TextView
    private lateinit var leftLanguageDropdown: Spinner
    private lateinit var rightLanguageDropdown: Spinner

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Translate, SpeechRecognition, and TextToSpeechManager
        translate = Translate()
        speechRecognition = SpeechRecognition(this)
        textToSpeechManager = TextToSpeechManager(this)
        audioPlayer = AudioPlayer(this)

        // Find UI elements by ID
        val leftButton: Button = findViewById(R.id.leftButton)
        val rightButton: Button = findViewById(R.id.rightButton)
        leftTextBox = findViewById(R.id.leftTextBox)
        rightTextBox = findViewById(R.id.rightTextBox)
        leftLanguageDropdown = findViewById(R.id.leftLanguageDropdown)
        rightLanguageDropdown = findViewById(R.id.rightLanguageDropdown)

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

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission required to use speech recognition", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleButtonClick(isLeft: Boolean) {
        val sourceLanguage = if (isLeft) leftLanguageDropdown.selectedItem.toString() else rightLanguageDropdown.selectedItem.toString()
        val targetLanguage = if (isLeft) rightLanguageDropdown.selectedItem.toString() else leftLanguageDropdown.selectedItem.toString()

        speechRecognition.recognizeSpeech { recognizedText ->
            // Update the left or right text box
            if (isLeft) {
                leftTextBox.text = recognizedText
            } else {
                rightTextBox.text = recognizedText
            }

            translate.translateText(recognizedText, sourceLanguage, targetLanguage) { translatedText ->
                // Ensure UI updates happen on the main thread
                runOnUiThread {
                    if (isLeft) {
                        rightTextBox.text = translatedText
                    } else {
                        leftTextBox.text = translatedText
                    }

                    audioPlayer.setAudioRouting(isLeft)

                    // Save the translated text to a file
                    textToSpeechManager.saveTextToSpeechToFile(translatedText)

                    // Speak the translated text
                    textToSpeechManager.speak(translatedText)
                }
            }
        }
    }



    private fun getLanguagesArray(): Array<String> {
        return arrayOf("English", "Spanish", "French", "German", "Hindi")
    }




    override fun onDestroy() {
        super.onDestroy()
        speechRecognition.destroy()
        textToSpeechManager.shutdown()
    }
}
