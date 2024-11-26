package com.example.translationapp

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.translationapp.ui.theme.TranslationAppTheme
import Translate
//import TranslatorManager
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale


class MainActivity : ComponentActivity() {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var translate: Translate
    private lateinit var speechRecognition: SpeechRecognition
    private lateinit var audioPlayer: AudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize essential components
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.ENGLISH
            } else {
                Toast.makeText(this, "TTS Initialization failed!", Toast.LENGTH_SHORT).show()
            }
        }
        translate = Translate()
        speechRecognition = SpeechRecognition(this)
        audioPlayer = AudioPlayer(this, textToSpeech)

        // Check for audio permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 200)
        }

        // Set Jetpack Compose content
        setContent {
            TranslationAppUI()
        }
    }

    @Composable
    fun TranslationAppUI() {
        val context = LocalContext.current

        // State variables for inputs and outputs
        var leftText by remember { mutableStateOf("") }
        var rightText by remember { mutableStateOf("") }
        var leftLanguage by remember { mutableStateOf("English") }
        var rightLanguage by remember { mutableStateOf("Spanish") }
        val languages = getLanguagesArray()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language Dropdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LanguageDropdown(
                        label = "Left Language",
                        selectedLanguage = leftLanguage,
                        onLanguageSelected = { leftLanguage = it },
                        languages = languages
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LanguageDropdown(
                        label = "Right Language",
                        selectedLanguage = rightLanguage,
                        onLanguageSelected = { rightLanguage = it },
                        languages = languages
                    )
                }
            }

            // Text Inputs and Buttons for Translation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = leftText,
                        onValueChange = { leftText = it },
                        label = { Text("Left Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (leftText.isNotEmpty()) {
                                handleTextInput(
                                    inputText = leftText,
                                    sourceLanguage = leftLanguage,
                                    targetLanguage = rightLanguage,
                                    onTranslationResult = { rightText = it }
                                )
                            } else {
                                Toast.makeText(context, "Enter text to translate.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Left Send")
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = rightText,
                        onValueChange = { rightText = it },
                        label = { Text("Right Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (rightText.isNotEmpty()) {
                                handleTextInput(
                                    inputText = rightText,
                                    sourceLanguage = rightLanguage,
                                    targetLanguage = leftLanguage,
                                    onTranslationResult = { leftText = it }
                                )
                            } else {
                                Toast.makeText(context, "Enter text to translate.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Right Send")
                    }
                }
            }

            // Buttons for Speech Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        handleSpeechInput(
                            sourceLanguage = leftLanguage,
                            targetLanguage = rightLanguage,
                            isLeft = true,
                            onResult = { leftText = it }
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Left Speech")
                }
                Button(
                    onClick = {
                        handleSpeechInput(
                            sourceLanguage = rightLanguage,
                            targetLanguage = leftLanguage,
                            isLeft = false,
                            onResult = { rightText = it }
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Right Speech")
                }
            }
        }
    }

    @Composable
    fun LanguageDropdown(label: String, selectedLanguage: String, onLanguageSelected: (String) -> Unit, languages: Array<String>) {
        var expanded by remember { mutableStateOf(false) }

        Column {
            Text(label, style = MaterialTheme.typography.caption)
            Box {
                Text(
                    text = selectedLanguage,
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                    style = MaterialTheme.typography.body1
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    languages.forEach { language ->
                        DropdownMenuItem(onClick = {
                            onLanguageSelected(language)
                            expanded = false
                        }) {
                            Text(language)
                        }
                    }
                }
            }
        }
    }

//    private fun checkPermissions() {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
//        }
//    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
//            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "Permission required to use speech recognition", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    private fun handleTextInput(inputText: String, sourceLanguage: String, targetLanguage: String, onTranslationResult: (String) -> Unit) {
        translate.translateText(inputText, sourceLanguage, targetLanguage) { translatedText ->
            runOnUiThread { onTranslationResult(translatedText) }
            val locale = Locale.forLanguageTag(getLanguageCode(targetLanguage))
            audioPlayer.speakThroughEarphone(translatedText, true, locale)
        }
    }


    private fun handleSpeechInput(sourceLanguage: String, targetLanguage: String, isLeft: Boolean, onResult: (String) -> Unit) {
        val sourceLanguageCode = getLanguageCodeforSR(sourceLanguage)
        speechRecognition.recognizeSpeech(sourceLanguageCode) { recognizedText ->
            onResult(recognizedText)
            translate.translateText(recognizedText, sourceLanguage, targetLanguage) { translatedText ->
                runOnUiThread { if (isLeft) onResult(translatedText) }
                val locale = Locale.forLanguageTag(getLanguageCode(targetLanguage))
                audioPlayer.speakThroughEarphone(translatedText, isLeft, locale)
            }
        }
    }

    private fun getLanguageCodeforSR(sourceLanguage: String): String {
        return when (sourceLanguage.lowercase()) {
            "english" -> "en-US"
            "spanish" -> "es-ES"
            "french" -> "fr-FR"
            "german" -> "de-DE"
            "hindi" -> "hi-IN"
            "arabic" -> "ar-SA"
            "bengali" -> "bn-IN"
            "chinese" -> "zh-CN"
            "dutch" -> "nl-NL"
            "italian" -> "it-IT"
            "japanese" -> "ja-JP"
            "korean" -> "ko-KR"
            "malay" -> "ms-MY"
            "portuguese" -> "pt-PT"
            "russian" -> "ru-RU"
            "turkish" -> "tr-TR"
            "vietnamese" -> "vi-VN"
            "thai" -> "th-TH"
            "filipino" -> "fil-PH"
            "swedish" -> "sv-SE"
            "norwegian" -> "no-NO"
            "danish" -> "da-DK"
            "finnish" -> "fi-FI"
            "hebrew" -> "he-IL"
            "swahili" -> "sw-TZ"
            "ukrainian" -> "uk-UA"
            "czech" -> "cs-CZ"
            "hungarian" -> "hu-HU"
            "romanian" -> "ro-RO"
            "slovak" -> "sk-SK"
            "bulgarian" -> "bg-BG"
            "croatian" -> "hr-HR"
            "serbian" -> "sr-RS"
            "slovenian" -> "sl-SI"
            "lithuanian" -> "lt-LT"
            "latvian" -> "lv-LV"
            "estonian" -> "et-EE"
            "persian" -> "fa-IR"
            "telugu" -> "te-IN"
            "tamil" -> "ta-IN"
            "marathi" -> "mr-IN"
            "kannada" -> "kn-IN"
            "punjabi" -> "pa-IN"
            "gujarati" -> "gu-IN"
            "burmese" -> "my-MM"
            "armenian" -> "hy-AM"
            "georgian" -> "ka-GE"
            "khmer" -> "km-KH"
            "lao" -> "lo-LA"
            "malagasy" -> "mg-MG"
            "sinhala" -> "si-LK"
            "tigrinya" -> "ti-ER"
            "yiddish" -> "yi-YI"
            else -> "en-US"  // Default to English (US)
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
