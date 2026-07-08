package com.axveer.lancar.platform

interface AudioPlayer {
    suspend fun play(fileName: String)
}

class NoopAudioPlayer : AudioPlayer {
    override suspend fun play(fileName: String) { /* no-op */ }
}
