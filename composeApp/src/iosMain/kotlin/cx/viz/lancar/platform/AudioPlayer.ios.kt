package cx.viz.lancar.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import lancar.composeapp.generated.resources.Res
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosAudioPlayer : AudioPlayer {
    private var player: AVAudioPlayer? = null
    private val mutex = Mutex()
    private var sessionConfigured = false

    override suspend fun play(fileName: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            configureSessionOnce()
            val bytes = Res.readBytes("files/audio/$fileName")
            val data = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            player = AVAudioPlayer(data = data, error = null)
            if (player == null) return@withLock
            player?.prepareToPlay()
            player?.play()
        }
    }

    // Default category (soloAmbient) is silenced by the hardware mute switch on a real
    // device — playback is inaudible unless the ringer is on. The simulator has no such
    // switch, so this only reproduces on device. `.playback` plays regardless of mute.
    private fun configureSessionOnce() {
        if (sessionConfigured) return
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)
        sessionConfigured = true
    }
}
