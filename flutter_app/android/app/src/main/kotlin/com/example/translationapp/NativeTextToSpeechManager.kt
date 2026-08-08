package com.example.translationapp

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class NativeTextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context, this)
    private val pendingPlayback = ConcurrentHashMap<String, PendingPlayback>()
    private var mediaPlayer: MediaPlayer? = null
    private var isReady = false
    private var currentLocale: Locale = Locale.getDefault()
    private var pendingSpeak: PendingSpeak? = null

    private data class PendingSpeak(val text: String, val pan: Float)
    private data class PendingPlayback(val file: File, val pan: Float)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return

        isReady = true
        configurePlaybackListener()
        applyLanguage(currentLocale)
        pendingSpeak?.let {
            pendingSpeak = null
            speak(it.text, it.pan)
        }
    }

    fun setLanguage(localeTag: String) {
        currentLocale = Locale.forLanguageTag(localeTag)
        if (isReady) {
            applyLanguage(currentLocale)
        }
    }

    fun speak(text: String, pan: Float) {
        if (text.isBlank()) return

        if (!isReady) {
            pendingSpeak = PendingSpeak(text, pan)
            return
        }

        val utteranceId = "tts-${System.nanoTime()}"
        val file = File(context.cacheDir, "$utteranceId.wav")
        val safePan = pan.coerceIn(-1.0f, 1.0f)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, safePan)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        pendingPlayback[utteranceId] = PendingPlayback(file, safePan)
        val result = textToSpeech.synthesizeToFile(text, params, file, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            pendingPlayback.remove(utteranceId)
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    fun shutdown() {
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun configurePlaybackListener() {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                val playback = pendingPlayback.remove(utteranceId) ?: return
                playSynthesizedFile(playback)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                pendingPlayback.remove(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                pendingPlayback.remove(utteranceId)
            }
        })
    }

    private fun applyLanguage(locale: Locale) {
        val result = textToSpeech.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            return
        }

        selectBestVoice(locale)?.let { voice ->
            textToSpeech.setVoice(voice)
        }
    }

    private fun selectBestVoice(locale: Locale): Voice? {
        return textToSpeech.voices
            ?.filter { it.locale.language == locale.language }
            ?.filterNot { it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true }
            ?.maxByOrNull { voiceScore(it, locale) }
    }

    private fun voiceScore(voice: Voice, locale: Locale): Int {
        var score = 0
        if (voice.locale == locale) {
            score += 500
        } else if (voice.locale.language == locale.language) {
            score += 250
        }
        score += voice.quality * 20
        if (voice.isNetworkConnectionRequired) {
            score += 120
        }
        score -= voice.latency
        return score
    }

    private fun playSynthesizedFile(playback: PendingPlayback) {
        try {
            val leftVolume: Float
            val rightVolume: Float
            if (playback.pan < 0f) {
                leftVolume = 1f
                rightVolume = 0f
            } else if (playback.pan > 0f) {
                leftVolume = 0f
                rightVolume = 1f
            } else {
                leftVolume = 1f
                rightVolume = 1f
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(playback.file.absolutePath)
                setVolume(leftVolume, rightVolume)
                setOnCompletionListener { player ->
                    player.release()
                    if (mediaPlayer === player) {
                        mediaPlayer = null
                    }
                    playback.file.delete()
                }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    if (mediaPlayer === player) {
                        mediaPlayer = null
                    }
                    playback.file.delete()
                    true
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            playback.file.delete()
        }
    }
}
