package com.vocalingo.app.conversation

object AudioRouting {
    fun listenerForSpeaker(speaker: SpeakerSide): SpeakerSide = speaker.opposite

    fun stereoGainsForListener(listener: SpeakerSide): Pair<Float, Float> {
        return when (listener) {
            SpeakerSide.LEFT_USER -> 1f to 0f
            SpeakerSide.RIGHT_USER -> 0f to 1f
        }
    }
}
