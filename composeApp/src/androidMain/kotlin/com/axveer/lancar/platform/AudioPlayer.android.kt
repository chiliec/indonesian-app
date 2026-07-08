package com.axveer.lancar.platform

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lancar.composeapp.generated.resources.Res
import java.io.File

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    private var player: MediaPlayer? = null

    override suspend fun play(fileName: String) = withContext(Dispatchers.IO) {
        val bytes = Res.readBytes("files/audio/$fileName")
        val tmp = File(context.cacheDir, fileName).apply { writeBytes(bytes) }
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(tmp.absolutePath)
            setOnCompletionListener { it.release() }
            prepare()
            start()
        }
        Unit
    }
}
