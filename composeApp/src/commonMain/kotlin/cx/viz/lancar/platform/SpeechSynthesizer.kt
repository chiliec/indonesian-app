package cx.viz.lancar.platform

interface SpeechSynthesizer {
    suspend fun speak(text: String, lang: String = "id-ID")
}

class NoopSpeechSynthesizer : SpeechSynthesizer {
    override suspend fun speak(text: String, lang: String) { /* no-op */ }
}
