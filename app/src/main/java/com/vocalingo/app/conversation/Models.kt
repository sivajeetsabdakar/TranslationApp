package com.vocalingo.app.conversation

import android.util.Base64

enum class SpeakerSide {
    LEFT_USER,
    RIGHT_USER;

    val opposite: SpeakerSide
        get() = if (this == LEFT_USER) RIGHT_USER else LEFT_USER
}

enum class VadState {
    SILENCE,
    SPEECH
}

enum class CalibrationResult {
    STEREO_OK,
    MONO_OUTPUT_DETECTED,
    UNKNOWN_BLUETOOTH_BEHAVIOR
}

data class AppLanguage(
    val id: String,
    val displayName: String,
    val sttCode: String,
    val translationCode: String,
    val ttsCode: String,
)

data class ConversationEvent(
    val type: ConversationEventType,
    val side: SpeakerSide,
    val sourceLanguage: String,
    val targetLanguage: String,
    val text: String = "",
    val segmentId: String = "",
    val audioBytes: ByteArray = ByteArray(0),
    val latencyMs: Long = 0L,
    val isFinal: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ConversationEvent
        return type == other.type &&
            side == other.side &&
            sourceLanguage == other.sourceLanguage &&
            targetLanguage == other.targetLanguage &&
            text == other.text &&
            segmentId == other.segmentId &&
            audioBytes.contentEquals(other.audioBytes) &&
            latencyMs == other.latencyMs &&
            isFinal == other.isFinal
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + side.hashCode()
        result = 31 * result + sourceLanguage.hashCode()
        result = 31 * result + targetLanguage.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + segmentId.hashCode()
        result = 31 * result + audioBytes.contentHashCode()
        result = 31 * result + latencyMs.hashCode()
        result = 31 * result + isFinal.hashCode()
        return result
    }
}

enum class ConversationEventType {
    PARTIAL_TRANSCRIPT,
    SEGMENT_COMMITTED,
    TRANSLATED_TEXT,
    TTS_AUDIO,
    LATENCY_METRICS,
    SESSION_ERROR
}

object LanguageCatalog {
    val indianFirstLanguages = listOf(
        AppLanguage("en-IN", "English (India)", "en-IN", "en", "en-IN"),
        AppLanguage("en-US", "English (US)", "en-US", "en", "en-US"),
        AppLanguage("hi-IN", "Hindi", "hi-IN", "hi", "hi-IN"),
        AppLanguage("bn-IN", "Bengali", "bn-IN", "bn", "bn-IN"),
        AppLanguage("ta-IN", "Tamil", "ta-IN", "ta", "ta-IN"),
        AppLanguage("te-IN", "Telugu", "te-IN", "te", "te-IN"),
        AppLanguage("mr-IN", "Marathi", "mr-IN", "mr", "mr-IN"),
        AppLanguage("gu-IN", "Gujarati", "gu-IN", "gu", "gu-IN"),
        AppLanguage("kn-IN", "Kannada", "kn-IN", "kn", "kn-IN"),
        AppLanguage("ml-IN", "Malayalam", "ml-IN", "ml", "ml-IN"),
        AppLanguage("pa-IN", "Punjabi", "pa-IN", "pa", "pa-IN"),
        AppLanguage("ur-IN", "Urdu", "ur-IN", "ur", "ur-IN"),
    )

    fun byId(id: String): AppLanguage = indianFirstLanguages.firstOrNull { it.id == id }
        ?: indianFirstLanguages.first()
}

fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

fun String.base64ToBytes(): ByteArray = Base64.decode(this, Base64.DEFAULT)
