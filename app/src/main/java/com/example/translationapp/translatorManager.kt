import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import com.example.translationapp.SpeechRecognition
import java.io.File

class TranslatorManager(
    private val speechRecognition: SpeechRecognition,
    private val textToSpeech: TextToSpeech,
    private val translate: Translate // Inject Translate class
) {
    private val translationQueue: MutableList<String> = mutableListOf()
    private var isSpeaking = false

    fun processSpeechInput(inputText: String, sourceLang: String, targetLang: String, isLeft: Boolean) {
        // Combine new text with pending translation if necessary
        if (!isSpeaking || translationQueue.isEmpty()) {
            translate.translateText(inputText, sourceLang, targetLang) { translatedText ->
                translationQueue.add(translatedText)
                if (!isSpeaking) {
                    playTranslationQueue(sourceLang, targetLang, isLeft)
                }
            }
        } else {
            // Optionally combine partial input into ongoing translation
            translationQueue[translationQueue.size - 1] += " $inputText"
        }
    }


    private fun playTranslationQueue(sourceLang: String, targetLang: String, isLeft: Boolean) {
        if (translationQueue.isNotEmpty()) {
            isSpeaking = true
            val textToSpeak = translationQueue.removeAt(0)

            val audioFile = File("tts_output_${System.currentTimeMillis()}.wav")

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ttsOutput")
            }

            textToSpeech.synthesizeToFile(textToSpeak, params, audioFile, "ttsOutput")

            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "ttsOutput") {
                        playStereoAudioWithPan(audioFile, isLeft) // Play through specified channel
                        audioFile.delete() // Clean up after playback
                    }
                    isSpeaking = false
                    if (translationQueue.isNotEmpty()) {
                        playTranslationQueue(sourceLang, targetLang, isLeft) // Play next in queue
                    } else {
                        // Resume listening after playback
                        speechRecognition.startListening(sourceLang) { input ->
                            processSpeechInput(input, sourceLang, targetLang, isLeft) // Loop back
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    Toast.makeText(null, "Error synthesizing audio", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun playStereoAudioWithPan(audioFile: File, isLeft: Boolean) {
        val monoData = audioFile.readBytes()

        // Convert mono audio to stereo
        val stereoData = ByteArray(monoData.size * 2)
        for (i in monoData.indices step 2) {
            val sample = monoData[i].toInt() or (monoData[i + 1].toInt() shl 8)
            if (!isLeft) {
                // Left channel active, right channel silent
                stereoData[i * 2] = monoData[i] // Left channel
                stereoData[i * 2 + 1] = monoData[i + 1]
                stereoData[i * 2 + 2] = 0 // Right channel muted
                stereoData[i * 2 + 3] = 0
            } else {
                // Right channel active, left channel silent
                stereoData[i * 2] = 0 // Left channel muted
                stereoData[i * 2 + 1] = 0
                stereoData[i * 2 + 2] = monoData[i] // Right channel
                stereoData[i * 2 + 3] = monoData[i + 1]
            }
        }

        // Configure AudioTrack for stereo output
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            22000, // Sample rate
            AudioFormat.CHANNEL_OUT_STEREO, // Stereo audio
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit PCM
            stereoData.size,
            AudioTrack.MODE_STATIC
        )

        // Write and play stereo audio
        audioTrack.write(stereoData, 0, stereoData.size)
        audioTrack.play()
    }
}
