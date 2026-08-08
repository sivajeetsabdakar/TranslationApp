package com.example.translationapp

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val nativeChannelName = "translation_app/native"
    private val speechEventChannelName = "translation_app/speech_events"

    private var eventSink: EventChannel.EventSink? = null
    private lateinit var speechRecognition: GoogleCloudSpeechRecognition
    private lateinit var textToSpeechManager: NativeTextToSpeechManager

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        speechRecognition = GoogleCloudSpeechRecognition(this)
        textToSpeechManager = NativeTextToSpeechManager(this)

        EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            speechEventChannelName,
        ).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            nativeChannelName,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "speechStatus" -> result.success(
                    mapOf("apiKeyConfigured" to BuildConfig.GOOGLE_CLOUD_SPEECH_API_KEY.isNotBlank()),
                )

                "speak" -> {
                    val text = call.argument<String>("text").orEmpty()
                    val localeTag = call.argument<String>("localeTag") ?: "en-US"
                    val pan = (call.argument<Double>("pan") ?: 0.0).toFloat()
                    textToSpeechManager.setLanguage(localeTag)
                    textToSpeechManager.speak(text, pan)
                    result.success(null)
                }

                "startListening" -> {
                    val languageCode = call.argument<String>("languageCode") ?: "en-US"
                    startListening(languageCode, result)
                }

                "stopListening" -> {
                    speechRecognition.stop()
                    result.success(null)
                }

                else -> result.notImplemented()
            }
        }
    }

    private fun startListening(languageCode: String, result: MethodChannel.Result) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            result.error("MIC_PERMISSION", "Microphone permission is missing.", null)
            return
        }

        speechRecognition.start(
            languageCode,
            GoogleCloudSpeechRecognition.Callbacks(
                onPartial = { text -> sendSpeechEvent("partial", text) },
                onChunk = { text -> sendSpeechEvent("chunk", text) },
                onError = { message -> sendSpeechEvent("error", message) },
            ),
        )
        result.success(null)
    }

    private fun sendSpeechEvent(type: String, text: String) {
        runOnUiThread {
            eventSink?.success(mapOf("type" to type, "text" to text))
        }
    }

    override fun onDestroy() {
        speechRecognition.stop()
        textToSpeechManager.shutdown()
        super.onDestroy()
    }
}
