package cx.viz.lancar.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AndroidSpeechRecognizer(private val context: Context) : SpeechRecognizer {

    private val app = context.applicationContext

    override suspend fun isAvailable(): Boolean {
        if (!AndroidRecognizer.isRecognitionAvailable(app)) return false
        val granted = ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        return granted
    }

    override suspend fun recognize(lang: String): String? = withContext(Dispatchers.Main) {
        if (!AndroidRecognizer.isRecognitionAvailable(app)) return@withContext null
        suspendCancellableCoroutine { cont ->
            val recognizer = AndroidRecognizer.createSpeechRecognizer(app)
            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            var resumed = false
            fun finish(result: String?) {
                if (resumed) return
                resumed = true
                recognizer.destroy()
                cont.resume(result)
            }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val hits = results.getStringArrayList(AndroidRecognizer.RESULTS_RECOGNITION)
                    finish(hits?.firstOrNull())
                }
                override fun onError(error: Int) = finish(null)
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            cont.invokeOnCancellation { if (!resumed) { resumed = true; recognizer.destroy() } }
            recognizer.startListening(intent)
        }
    }
}
