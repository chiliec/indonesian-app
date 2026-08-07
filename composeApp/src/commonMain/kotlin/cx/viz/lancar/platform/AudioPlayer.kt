package cx.viz.lancar.platform

interface AudioPlayer {
    /**
     * Plays [fileName] at [rate] (1f = normal speed, 0.6f = slow).
     * Returns the clip's playback duration in milliseconds, or 0 if unknown.
     */
    suspend fun play(fileName: String, rate: Float = 1f): Long
}

class NoopAudioPlayer : AudioPlayer {
    override suspend fun play(fileName: String, rate: Float): Long = 0
}
