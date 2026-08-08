package com.vocalingo.app.network

import com.vocalingo.app.conversation.AppLanguage
import com.vocalingo.app.conversation.ConversationEvent
import com.vocalingo.app.conversation.ConversationEventType
import com.vocalingo.app.conversation.SpeakerSide
import com.vocalingo.app.conversation.VadState
import com.vocalingo.app.conversation.base64ToBytes
import com.vocalingo.app.conversation.toBase64
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class RealtimeGateway(
    private val backendWsUrl: String,
    private val apiToken: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build(),
) {
    private var webSocket: WebSocket? = null
    private var sessionId: String = ""
    private var side: SpeakerSide = SpeakerSide.LEFT_USER
    private var sourceLanguage: AppLanguage? = null
    private var targetLanguage: AppLanguage? = null
    private var isOpen = false
    private val pendingMessages = ArrayDeque<String>()
    private val mutableEvents = MutableSharedFlow<ConversationEvent>(extraBufferCapacity = 128)

    val events: SharedFlow<ConversationEvent> = mutableEvents

    fun connect(
        side: SpeakerSide,
        sourceLanguage: AppLanguage,
        targetLanguage: AppLanguage,
    ) {
        close()
        this.sessionId = UUID.randomUUID().toString()
        this.side = side
        this.sourceLanguage = sourceLanguage
        this.targetLanguage = targetLanguage
        val requestBuilder = Request.Builder().url(backendWsUrl)
        if (apiToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiToken")
        }
        val request = requestBuilder.build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isOpen = true
                webSocket.send(
                    JSONObject()
                        .put("type", "startSession")
                        .put("sessionId", sessionId)
                        .put("speakerSide", side.name)
                        .put("sourceLang", sourceLanguage.sttCode)
                        .put("targetLang", targetLanguage.translationCode)
                        .put("targetTtsLang", targetLanguage.ttsCode)
                        .toString(),
                )
                while (pendingMessages.isNotEmpty()) {
                    webSocket.send(pendingMessages.removeFirst())
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseServerEvent(text)?.let { event ->
                    mutableEvents.tryEmit(event)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                mutableEvents.tryEmit(
                    ConversationEvent(
                        type = ConversationEventType.SESSION_ERROR,
                        side = side,
                        sourceLanguage = sourceLanguage.id,
                        targetLanguage = targetLanguage.id,
                        text = t.localizedMessage ?: "Realtime connection failed.",
                    ),
                )
            }
        })
    }

    fun sendAudioFrame(frame: ByteArray, vadState: VadState) {
        send(
            JSONObject()
                .put("type", "audioFrame")
                .put("sessionId", sessionId)
                .put("pcmFrame", frame.toBase64())
                .put("vadState", vadState.name)
                .put("sentAtMs", System.currentTimeMillis())
                .toString(),
        )
    }

    fun sendVadState(vadState: VadState) {
        send(
            JSONObject()
                .put("type", "vadState")
                .put("sessionId", sessionId)
                .put("vadState", vadState.name)
                .toString(),
        )
    }

    fun cancelPlayback() {
        send(JSONObject().put("type", "cancelPlayback").put("sessionId", sessionId).toString())
    }

    fun close() {
        send(JSONObject().put("type", "stopSession").put("sessionId", sessionId).toString())
        webSocket?.close(1000, "stopped")
        webSocket = null
        isOpen = false
        pendingMessages.clear()
    }

    private fun send(message: String) {
        val socket = webSocket ?: return
        if (isOpen) {
            socket.send(message)
        } else if (pendingMessages.size < 24) {
            pendingMessages.addLast(message)
        }
    }

    private fun parseServerEvent(raw: String): ConversationEvent? {
        val json = JSONObject(raw)
        val source = sourceLanguage ?: return null
        val target = targetLanguage ?: return null
        val eventType = when (json.optString("type")) {
            "partialTranscript" -> ConversationEventType.PARTIAL_TRANSCRIPT
            "segmentCommitted" -> ConversationEventType.SEGMENT_COMMITTED
            "translatedText" -> ConversationEventType.TRANSLATED_TEXT
            "ttsAudio" -> ConversationEventType.TTS_AUDIO
            "latencyMetrics" -> ConversationEventType.LATENCY_METRICS
            "sessionError" -> ConversationEventType.SESSION_ERROR
            else -> return null
        }
        return ConversationEvent(
            type = eventType,
            side = side,
            sourceLanguage = source.id,
            targetLanguage = target.id,
            text = json.optString("text"),
            segmentId = json.optString("segmentId"),
            audioBytes = json.optString("audioBase64").takeIf { it.isNotBlank() }?.base64ToBytes() ?: ByteArray(0),
            latencyMs = json.optLong("latencyMs", 0L),
            isFinal = json.optBoolean("isFinal", false),
        )
    }
}
