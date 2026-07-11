package cx.viz.lancar.platform

interface SpeechRecognizer {
    suspend fun isAvailable(): Boolean
    suspend fun recognize(lang: String = "id-ID"): String?
}

class NoopSpeechRecognizer : SpeechRecognizer {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun recognize(lang: String): String? = null
}
