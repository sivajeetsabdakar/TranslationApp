package com.vocalingo.app.conversation

import android.content.Context
import com.vocalingo.app.audio.PcmAudioRecorder
import com.vocalingo.app.audio.StereoPcmPlayer
import com.vocalingo.app.network.RealtimeGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class ConversationSession(
    context: Context,
    backendWsUrl: String,
    backendApiToken: String,
    private val scope: CoroutineScope,
) {
    private val gateway = RealtimeGateway(backendWsUrl, backendApiToken)
    private val recorder = PcmAudioRecorder(context.applicationContext)
    private val player = StereoPcmPlayer()
    private val mutableEvents = MutableSharedFlow<ConversationEvent>(extraBufferCapacity = 128)

    val events: SharedFlow<ConversationEvent> = mutableEvents

    init {
        player.start(scope)
        scope.launch {
            gateway.events.collect { event ->
                mutableEvents.emit(event)
                if (event.type == ConversationEventType.TTS_AUDIO && event.audioBytes.isNotEmpty()) {
                    player.enqueue(
                        StereoPcmPlayer.PlaybackItem(
                            monoPcm16 = event.audioBytes,
                            listenerSide = AudioRouting.listenerForSpeaker(event.side),
                        ),
                    )
                }
            }
        }
    }

    fun start(side: SpeakerSide, sourceLanguage: AppLanguage, targetLanguage: AppLanguage) {
        stop()
        gateway.connect(side, sourceLanguage, targetLanguage)
        recorder.start(
            scope = scope,
            onFrame = { frame, vadState ->
                gateway.sendVadState(vadState)
                gateway.sendAudioFrame(frame, vadState)
            },
            onError = { message ->
                mutableEvents.tryEmit(
                    ConversationEvent(
                        type = ConversationEventType.SESSION_ERROR,
                        side = side,
                        sourceLanguage = sourceLanguage.id,
                        targetLanguage = targetLanguage.id,
                        text = message,
                    ),
                )
            },
        )
    }

    fun stop() {
        recorder.stop()
        player.clear()
        gateway.cancelPlayback()
        gateway.close()
    }

    fun shutdown() {
        stop()
        player.stop()
    }
}
