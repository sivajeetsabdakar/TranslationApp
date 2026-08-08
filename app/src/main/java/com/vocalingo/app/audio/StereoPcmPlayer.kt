package com.vocalingo.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.vocalingo.app.conversation.AudioRouting
import com.vocalingo.app.conversation.SpeakerSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class StereoPcmPlayer {
    private val queue = Channel<PlaybackItem>(capacity = Channel.UNLIMITED)
    private var playbackJob: Job? = null
    private var audioTrack: AudioTrack? = null

    data class PlaybackItem(
        val monoPcm16: ByteArray,
        val listenerSide: SpeakerSide,
        val sampleRateHz: Int = 24_000,
    )

    fun start(scope: CoroutineScope) {
        if (playbackJob != null) return
        playbackJob = scope.launch(Dispatchers.IO) {
            for (item in queue) {
                playBlocking(item)
            }
        }
    }

    fun enqueue(item: PlaybackItem) {
        queue.trySend(item)
    }

    fun clear() {
        while (queue.tryReceive().isSuccess) {
        }
        releaseTrack()
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        clear()
    }

    private fun playBlocking(item: PlaybackItem) {
        val stereo = monoToStereo(item.monoPcm16, item.listenerSide)
        val minBuffer = AudioTrack.getMinBufferSize(
            item.sampleRateHz,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(item.sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuffer, stereo.size))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack = track
        track.play()
        track.write(stereo, 0, stereo.size)
        track.stop()
        track.release()
        if (audioTrack === track) audioTrack = null
    }

    fun monoToStereo(monoPcm16: ByteArray, listenerSide: SpeakerSide): ByteArray {
        val (leftGain, rightGain) = AudioRouting.stereoGainsForListener(listenerSide)
        val sampleCount = monoPcm16.size / 2
        val stereo = ByteArray(sampleCount * 4)
        for (index in 0 until sampleCount) {
            val low = monoPcm16[index * 2].toInt() and 0xFF
            val high = monoPcm16[index * 2 + 1].toInt()
            val sample = ((high shl 8) or low).toShort().toInt()
            val left = (sample * leftGain).toInt().toShort()
            val right = (sample * rightGain).toInt().toShort()
            writeShort(stereo, index * 4, left)
            writeShort(stereo, index * 4 + 2, right)
        }
        return stereo
    }

    private fun writeShort(target: ByteArray, offset: Int, value: Short) {
        val intValue = value.toInt()
        target[offset] = (intValue and 0xFF).toByte()
        target[offset + 1] = ((intValue ushr 8) and 0xFF).toByte()
    }

    private fun releaseTrack() {
        try {
            audioTrack?.stop()
        } catch (_: RuntimeException) {
        }
        audioTrack?.release()
        audioTrack = null
    }
}
