package cx.viz.lancar.platform

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import lancar.composeapp.generated.resources.Res
import java.io.File

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    private var player: MediaPlayer? = null
    private val mutex = Mutex()

    override suspend fun play(fileName: String, rate: Float): Long = withContext(Dispatchers.IO) {
        mutex.withLock {
            val bytes = Res.readBytes("files/audio/$fileName")
            val tmp = File(context.cacheDir, fileName).apply { writeBytes(bytes) }
            player?.release()
            player = null
            val mp = MediaPlayer()
            try {
                mp.setDataSource(tmp.absolutePath)
                mp.setOnCompletionListener { it.release() }
                mp.prepare()
                // Safe in the Prepared state; setting it while Paused would auto-start.
                if (rate != 1f) mp.playbackParams = mp.playbackParams.setSpeed(rate)
                mp.start()
                player = mp
                (mp.duration / rate).toLong()
            } catch (e: Exception) {
                mp.release()
                throw e
            }
        }
    }
}
