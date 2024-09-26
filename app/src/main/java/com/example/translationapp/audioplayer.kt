package com.example.translationapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build

class AudioPlayer(private val context: Context) {

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private lateinit var audioTrack: AudioTrack

    init {
        setupAudioTrack()
    }

    private fun setupAudioTrack() {
        // Set up the AudioTrack for stereo output
        val sampleRate = 44100 // Standard sample rate for music playback
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT // Standard 16-bit PCM audio

        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
        } else {
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        }
    }

    fun setAudioRouting(isLeftEarphone: Boolean) {
        if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
            if (isLeftEarphone) {
                // Set maximum volume on the left channel, mute the right channel
                audioTrack.setStereoVolume(1.0f, 0.0f)
            } else {
                // Set maximum volume on the right channel, mute the left channel
                audioTrack.setStereoVolume(0.0f, 1.0f)
            }
        }
    }

    fun playAudio(audioData: ByteArray) {
        if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
            audioTrack.play()
            audioTrack.write(audioData, 0, audioData.size)
        }
    }

    fun stopAudio() {
        if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
            audioTrack.stop()
            audioTrack.release()
        }
    }
}
