package com.vocalingo.app.audio

import com.vocalingo.app.conversation.SpeakerSide
import kotlinx.coroutines.CoroutineScope
import kotlin.math.PI
import kotlin.math.sin

class EarCalibrationPlayer(
    private val player: StereoPcmPlayer,
) {
    fun playLeftThenRight(scope: CoroutineScope) {
        player.start(scope)
        player.enqueue(StereoPcmPlayer.PlaybackItem(tone(), SpeakerSide.LEFT_USER))
        player.enqueue(StereoPcmPlayer.PlaybackItem(tone(frequencyHz = 880.0), SpeakerSide.RIGHT_USER))
    }

    private fun tone(frequencyHz: Double = 660.0, sampleRate: Int = 24_000, durationMs: Int = 450): ByteArray {
        val sampleCount = sampleRate * durationMs / 1000
        val output = ByteArray(sampleCount * 2)
        for (index in 0 until sampleCount) {
            val wave = sin(2.0 * PI * frequencyHz * index / sampleRate)
            val sample = (wave * Short.MAX_VALUE * 0.35).toInt().toShort()
            output[index * 2] = (sample.toInt() and 0xFF).toByte()
            output[index * 2 + 1] = ((sample.toInt() ushr 8) and 0xFF).toByte()
        }
        return output
    }
}
