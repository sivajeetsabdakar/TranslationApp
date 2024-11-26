package com.example.translationapp

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import java.io.File
import java.util.Locale

class AudioPlayer(private val context: Context, private val textToSpeech: TextToSpeech) {

    fun speakThroughEarphone(text: String, isLeft: Boolean, language: Locale) {
        val audioFile = File(context.filesDir, "tts_output.wav")

        // Set the desired language
        val result = textToSpeech.setLanguage(language)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "Selected language is not supported", Toast.LENGTH_SHORT).show()
            return
        }

        // Set a specific voice if available (optional)
        val voices = textToSpeech.voices
        val selectedVoice = voices.find { it.locale == language && !it.isNetworkConnectionRequired }
        selectedVoice?.let {
            textToSpeech.voice = it
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ttsOutput")
        }

        textToSpeech.synthesizeToFile(text, params, audioFile, "ttsOutput")

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "ttsOutput") {
                    playStereoAudioWithPan(audioFile, isLeft)
                }
            }

            override fun onError(utteranceId: String?) {
                Toast.makeText(context, "Failed to synthesize audio.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun playStereoAudioWithPan(audioFile: File, isLeft: Boolean) {
        val monoData = audioFile.readBytes()

        // Convert mono audio to stereo
        val stereoData = ByteArray(monoData.size * 2)
        for (i in monoData.indices step 2) {
            val sample = monoData[i].toInt() or (monoData[i + 1].toInt() shl 8)
            if (!isLeft) {
                // Left channel active, right channel silent
                stereoData[i * 2] = monoData[i] // Left channel
                stereoData[i * 2 + 1] = monoData[i + 1]
                stereoData[i * 2 + 2] = 0 // Right channel muted
                stereoData[i * 2 + 3] = 0
            } else {
                // Right channel active, left channel silent
                stereoData[i * 2] = 0 // Left channel muted
                stereoData[i * 2 + 1] = 0
                stereoData[i * 2 + 2] = monoData[i] // Right channel
                stereoData[i * 2 + 3] = monoData[i + 1]
            }
        }

        // Configure AudioTrack for stereo output
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            22000, // Sample rate
            AudioFormat.CHANNEL_OUT_STEREO, // Stereo audio
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit PCM
            stereoData.size,
            AudioTrack.MODE_STATIC
        )

        // Write and play stereo audio
        audioTrack.write(stereoData, 0, stereoData.size)
        audioTrack.play()
    }
}
