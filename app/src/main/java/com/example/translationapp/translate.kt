package com.example.translationapp

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class Translate {

    private val client = OkHttpClient()

    // Function to translate text using LibreTranslate API
    fun translateText(text: String, sourceLang: String, targetLang: String, callback: (String) -> Unit) {
        val requestBody = FormBody.Builder()
            .add("q", text)
            .add("source", mapLanguage(sourceLang))
            .add("target", mapLanguage(targetLang))
            .add("format", "text")
            .build()

        val request = Request.Builder()
            .url("http://10.0.2.2:5000/translate")  // Use the emulator IP address
//            .url("http://192.168.1.4:5000/translate") // D104 5G
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Translation failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "")
                    val translatedText = json.getString("translatedText")
                    callback(translatedText)
                } else {
                    callback("Translation failed: ${response.message}")
                }
            }
        })
    }

    // Map languages to their respective ISO language codes
    private fun mapLanguage(language: String): String {
        return when (language.toLowerCase()) {
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
            "hebrew" -> "iw"  // 'iw' is commonly used, though 'he' is also valid
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
            else -> "en"  // default to English if no match is found
        }
    }

}
