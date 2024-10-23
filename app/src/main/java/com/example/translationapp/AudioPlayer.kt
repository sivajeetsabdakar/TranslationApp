import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

class AudioPlayer(private val context: Context) {

    // Method to play the audio with stereo panning (left or right channel)
    fun playWithStereoPanning(isLeft: Boolean, audioData: ByteArray) {
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            44100,  // Sample rate in Hz
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioData.size,
            AudioTrack.MODE_STREAM
        )

        // Set stereo volume based on the channel (left or right)
        if (isLeft) {
            audioTrack.setStereoVolume(1.0f, 0.0f)  // Left ear only
        } else {
            audioTrack.setStereoVolume(0.0f, 1.0f)  // Right ear only
        }

        audioTrack.play()
        audioTrack.write(audioData, 0, audioData.size)
        audioTrack.stop()
        audioTrack.release()
    }
}
