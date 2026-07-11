package cx.viz.lancar.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidSpeechSynthesizer(context: Context) : SpeechSynthesizer {

    private val indonesian = Locale("id", "ID")
    @Volatile private var ready = false

    private val tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val res = engine?.setLanguage(indonesian)
            ready = res != TextToSpeech.LANG_MISSING_DATA &&
                    res != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }
    private val engine: TextToSpeech? get() = tts

    override suspend fun speak(text: String, lang: String) {
        if (!ready || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lancar-tts")
    }
}
