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
//            .url("http://10.0.2.2:5000/translate")  // Use the emulator IP address
            .url("http://192.168.1.6:5000/translate") // D104 5G
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
        return when (language.lowercase()) {
            "english" -> "en"
            "arabic" -> "ar"
            "azerbaijani" -> "az"
            "bulgarian" -> "bg"
            "bengali" -> "bn"
            "catalan" -> "ca"
            "czech" -> "cs"
            "danish" -> "da"
            "german" -> "de"
            "greek" -> "el"
            "esperanto" -> "eo"
            "spanish" -> "es"
            "estonian" -> "et"
            "persian" -> "fa"
            "finnish" -> "fi"
            "french" -> "fr"
            "irish" -> "ga"
            "hebrew" -> "he" // 'iw' is also valid
            "hindi" -> "hi"
            "hungarian" -> "hu"
            "indonesian" -> "id"
            "italian" -> "it"
            "japanese" -> "ja"
            "korean" -> "ko"
            "lithuanian" -> "lt"
            "latvian" -> "lv"
            "malay" -> "ms"
            "norwegian bokmål" -> "nb"
            "dutch" -> "nl"
            "polish" -> "pl"
            "portuguese" -> "pt"
            "romanian" -> "ro"
            "russian" -> "ru"
            "slovak" -> "sk"
            "slovenian" -> "sl"
            "albanian" -> "sq"
            "swedish" -> "sv"
            "thai" -> "th"
            "filipino" -> "tl"
            "turkish" -> "tr"
            "ukrainian" -> "uk"
            "urdu" -> "ur"
            "chinese" -> "zh"
            "chinese (traditional)" -> "zt"
            else -> "en"
        }
    }


}
