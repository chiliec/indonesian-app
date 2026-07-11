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

/**
 * @param requestMicPermission suspends until the user resolves a runtime RECORD_AUDIO prompt,
 *   returning whether it was granted. Supplied by the host Activity (see [MainActivity]); `null`
 *   in contexts without an Activity (then recognition is only possible if already granted).
 */
class AndroidSpeechRecognizer(
    private val context: Context,
    private val requestMicPermission: (suspend () -> Boolean)? = null,
) : SpeechRecognizer {

    private val app = context.applicationContext

    private fun micGranted(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    // Availability reflects only whether a recognition service exists. The RECORD_AUDIO grant is
    // handled contextually in [recognize] (requested on first mic tap) so the mic button can
    // surface before permission is granted — otherwise there is no in-app path to ever grant it.
    override suspend fun isAvailable(): Boolean = AndroidRecognizer.isRecognitionAvailable(app)

    override suspend fun recognize(lang: String): String? = withContext(Dispatchers.Main) {
        if (!AndroidRecognizer.isRecognitionAvailable(app)) return@withContext null
        val granted = micGranted() || (requestMicPermission?.invoke() ?: false)
        if (!granted) return@withContext null
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
