package com.vocalingo.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import com.vocalingo.app.conversation.VadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PcmAudioRecorder(
    private val context: Context,
    private val vad: VoiceActivityDetector = VoiceActivityDetector(),
) {
    companion object {
        const val SAMPLE_RATE_HZ = 16_000
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    fun start(scope: CoroutineScope, onFrame: (ByteArray, VadState) -> Unit, onError: (String) -> Unit) {
        stop()
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Microphone permission is missing.")
            return
        }
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) {
            onError("Microphone is not available.")
            return
        }
        val bufferSamples = SAMPLE_RATE_HZ / 10
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, bufferSamples * 2),
        )
        audioRecord = record
        try {
            record.startRecording()
        } catch (error: IllegalStateException) {
            onError("Could not start microphone: ${error.localizedMessage ?: "unknown error"}")
            stop()
            return
        }
        recordingJob = scope.launch(Dispatchers.IO) {
            val samples = ShortArray(bufferSamples)
            while (isActive) {
                val read = record.read(samples, 0, samples.size)
                if (read <= 0) continue
                onFrame(shortsToLittleEndianBytes(samples, read), vad.detect(samples, read))
            }
        }
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
        } catch (_: RuntimeException) {
        }
        audioRecord?.release()
        audioRecord = null
    }

    private fun shortsToLittleEndianBytes(samples: ShortArray, count: Int): ByteArray {
        val bytes = ByteArray(count * 2)
        for (index in 0 until count) {
            val sample = samples[index].toInt()
            bytes[index * 2] = (sample and 0xFF).toByte()
            bytes[index * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
