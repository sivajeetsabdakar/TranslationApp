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
            else -> "en" // Default to English if not found
        }
    }
}
