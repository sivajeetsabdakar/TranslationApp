package com.example.translationapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechGrpc
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import kotlin.concurrent.thread

class GoogleCloudSpeechRecognition(private val context: Context) {
    data class Callbacks(
        val onPartial: (String) -> Unit = {},
        val onChunk: (String) -> Unit,
        val onError: (String) -> Unit = {},
    )

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var channel: ManagedChannel? = null
    private var audioRecord: AudioRecord? = null
    private var requestObserver: StreamObserver<StreamingRecognizeRequest>? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var isRecording = false

    fun start(languageCode: String, callbacks: Callbacks) {
        if (isRecording) return

        val apiKey = BuildConfig.GOOGLE_CLOUD_SPEECH_API_KEY.trim()
        if (apiKey.isBlank()) {
            callbacks.onError("Add googleCloudSpeechApiKey to local.properties, then rebuild the debug app.")
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callbacks.onError("Microphone permission is missing.")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) {
            callbacks.onError("Microphone is not available.")
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize * 2,
        )

        channel = OkHttpChannelBuilder
            .forAddress("speech.googleapis.com", 443)
            .build()

        val metadata = Metadata().apply {
            put(Metadata.Key.of("x-goog-api-key", Metadata.ASCII_STRING_MARSHALLER), apiKey)
        }
        val speechStub = SpeechGrpc
            .newStub(channel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))

        val responseObserver = object : StreamObserver<StreamingRecognizeResponse> {
            override fun onNext(response: StreamingRecognizeResponse) {
                response.resultsList.forEach { result ->
                    val text = result.alternativesList.firstOrNull()?.transcript?.trim().orEmpty()
                    if (text.isBlank()) return@forEach

                    if (result.isFinal) {
                        callbacks.onChunk(text)
                    } else {
                        callbacks.onPartial(text)
                    }
                }
            }

            override fun onError(t: Throwable) {
                isRecording = false
                callbacks.onError("Google speech failed: ${t.localizedMessage ?: t.javaClass.simpleName}")
                releaseAudio()
            }

            override fun onCompleted() {
                isRecording = false
                releaseAudio()
            }
        }

        requestObserver = speechStub.streamingRecognize(responseObserver)
        requestObserver?.onNext(streamingConfigRequest(languageCode))

        try {
            audioRecord?.startRecording()
        } catch (e: IllegalStateException) {
            callbacks.onError("Could not start microphone: ${e.localizedMessage ?: "unknown error"}")
            stop()
            return
        }

        isRecording = true
        recordingThread = thread(start = true, name = "GoogleCloudSpeechRecognition") {
            processAudio(callbacks)
        }
    }

    fun stop() {
        isRecording = false
        try {
            requestObserver?.onCompleted()
        } catch (_: RuntimeException) {
        }
        requestObserver = null
        releaseAudio()
        channel?.shutdownNow()
        channel = null
        recordingThread = null
    }

    private fun processAudio(callbacks: Callbacks) {
        val buffer = ShortArray((0.1 * sampleRate).toInt())

        while (isRecording) {
            val read = try {
                audioRecord?.read(buffer, 0, buffer.size) ?: 0
            } catch (e: RuntimeException) {
                callbacks.onError("Microphone read failed: ${e.localizedMessage ?: "unknown error"}")
                stop()
                return
            }

            if (read <= 0) continue

            val audioBytes = shortsToLittleEndianBytes(buffer, read)
            try {
                requestObserver?.onNext(
                    StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(audioBytes))
                        .build(),
                )
            } catch (e: RuntimeException) {
                callbacks.onError("Could not send audio to Google: ${e.localizedMessage ?: "unknown error"}")
                stop()
                return
            }
        }
    }

    private fun streamingConfigRequest(languageCode: String): StreamingRecognizeRequest {
        val recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(sampleRate)
            .setLanguageCode(languageCode)
            .setEnableAutomaticPunctuation(true)
            .build()

        val streamingConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(recognitionConfig)
            .setInterimResults(true)
            .setSingleUtterance(false)
            .build()

        return StreamingRecognizeRequest.newBuilder()
            .setStreamingConfig(streamingConfig)
            .build()
    }

    private fun shortsToLittleEndianBytes(samples: ShortArray, count: Int): ByteArray {
        val bytes = ByteArray(count * 2)
        for (index in 0 until count) {
            val sample = samples[index].toInt()
            bytes[index * 2] = (sample and 0x00FF).toByte()
            bytes[index * 2 + 1] = ((sample and 0xFF00) shr 8).toByte()
        }
        return bytes
    }

    private fun releaseAudio() {
        try {
            audioRecord?.stop()
        } catch (_: RuntimeException) {
        }
        audioRecord?.release()
        audioRecord = null
    }
}
