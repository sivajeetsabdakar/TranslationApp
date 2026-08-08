package com.vocalingo.app.audio

import com.vocalingo.app.conversation.VadState
import kotlin.math.abs

class VoiceActivityDetector(
    private val speechThreshold: Int = 900,
    private val hangoverFrames: Int = 6,
) {
    private var trailingSpeechFrames = 0

    fun detect(samples: ShortArray, count: Int): VadState {
        if (count <= 0) return VadState.SILENCE
        var sum = 0L
        for (index in 0 until count) {
            sum += abs(samples[index].toInt())
        }
        val averageAmplitude = sum / count
        val hasSpeech = averageAmplitude >= speechThreshold
        if (hasSpeech) {
            trailingSpeechFrames = hangoverFrames
            return VadState.SPEECH
        }
        if (trailingSpeechFrames > 0) {
            trailingSpeechFrames--
            return VadState.SPEECH
        }
        return VadState.SILENCE
    }
}
