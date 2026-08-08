package com.vocalingo.app

import com.vocalingo.app.audio.StereoPcmPlayer
import com.vocalingo.app.audio.VoiceActivityDetector
import com.vocalingo.app.conversation.AudioRouting
import com.vocalingo.app.conversation.LanguageCatalog
import com.vocalingo.app.conversation.SpeakerSide
import com.vocalingo.app.conversation.VadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationUnitTest {
    @Test
    fun routesSpeakerToOppositeListener() {
        assertEquals(SpeakerSide.RIGHT_USER, AudioRouting.listenerForSpeaker(SpeakerSide.LEFT_USER))
        assertEquals(SpeakerSide.LEFT_USER, AudioRouting.listenerForSpeaker(SpeakerSide.RIGHT_USER))
    }

    @Test
    fun appliesLeftAndRightStereoGains() {
        assertEquals(1f to 0f, AudioRouting.stereoGainsForListener(SpeakerSide.LEFT_USER))
        assertEquals(0f to 1f, AudioRouting.stereoGainsForListener(SpeakerSide.RIGHT_USER))
    }

    @Test
    fun languageCatalogContainsIndianFirstSet() {
        val ids = LanguageCatalog.indianFirstLanguages.map { it.id }.toSet()
        assertTrue(ids.contains("hi-IN"))
        assertTrue(ids.contains("ta-IN"))
        assertTrue(ids.contains("bn-IN"))
        assertTrue(ids.contains("ur-IN"))
    }

    @Test
    fun detectsSpeechByAmplitude() {
        val detector = VoiceActivityDetector(speechThreshold = 100, hangoverFrames = 0)
        assertEquals(VadState.SILENCE, detector.detect(shortArrayOf(1, 2, 3), 3))
        assertEquals(VadState.SPEECH, detector.detect(shortArrayOf(200, 250, 300), 3))
    }

    @Test
    fun convertsMonoPcmToTargetStereoChannel() {
        val player = StereoPcmPlayer()
        val mono = byteArrayOf(0x34, 0x12)
        val rightOnly = player.monoToStereo(mono, SpeakerSide.RIGHT_USER)
        assertEquals(4, rightOnly.size)
        assertEquals(0, rightOnly[0].toInt())
        assertEquals(0, rightOnly[1].toInt())
        assertEquals(0x34, rightOnly[2].toInt() and 0xFF)
        assertEquals(0x12, rightOnly[3].toInt() and 0xFF)
    }

    @Test
    fun convertsMonoPcmToLeftStereoChannel() {
        val player = StereoPcmPlayer()
        val mono = byteArrayOf(0x78, 0x56)
        val leftOnly = player.monoToStereo(mono, SpeakerSide.LEFT_USER)
        assertEquals(4, leftOnly.size)
        assertEquals(0x78, leftOnly[0].toInt() and 0xFF)
        assertEquals(0x56, leftOnly[1].toInt() and 0xFF)
        assertEquals(0, leftOnly[2].toInt())
        assertEquals(0, leftOnly[3].toInt())
    }
}
