package cx.viz.lancar.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioEngine
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import kotlin.coroutines.resume

private val kAuthNotDetermined = SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusNotDetermined
private val kAuthAuthorized = SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized

@OptIn(ExperimentalForeignApi::class)
class IosSpeechRecognizer : SpeechRecognizer {

    private val recognizer = SFSpeechRecognizer(NSLocale("id-ID"))

    override suspend fun isAvailable(): Boolean {
        val status = when (SFSpeechRecognizer.authorizationStatus()) {
            kAuthNotDetermined -> requestAuthorization()
            else -> SFSpeechRecognizer.authorizationStatus()
        }
        if (status != kAuthAuthorized) return false
        val rec = recognizer ?: return false
        return rec.isAvailable() && rec.supportsOnDeviceRecognition()
    }

    private suspend fun requestAuthorization(): SFSpeechRecognizerAuthorizationStatus =
        suspendCancellableCoroutine { cont ->
            SFSpeechRecognizer.requestAuthorization { status -> cont.resume(status) }
        }

    override suspend fun recognize(lang: String): String? = withContext(Dispatchers.Main) {
        val rec = recognizer ?: return@withContext null
        if (!rec.isAvailable()) return@withContext null

        val engine = AVAudioEngine()
        val request = SFSpeechAudioBufferRecognitionRequest().apply {
            requiresOnDeviceRecognition = true
            shouldReportPartialResults = false
        }

        suspendCancellableCoroutine { cont ->
            var resumed = false
            var task: platform.Speech.SFSpeechRecognitionTask? = null

            fun finish(result: String?) {
                if (resumed) return
                resumed = true
                engine.stop()
                engine.inputNode.removeTapOnBus(0u)
                request.endAudio()
                task?.cancel()
                cont.resume(result)
            }

            val input = engine.inputNode
            val format = input.outputFormatForBus(0u)
            input.installTapOnBus(0u, 1024u, format) { buffer, _ ->
                buffer?.let { request.appendAudioPCMBuffer(it) }
            }
            engine.prepare()
            engine.startAndReturnError(null)

            task = rec.recognitionTaskWithRequest(request) { res, err ->
                if (err != null) { finish(null); return@recognitionTaskWithRequest }
                if (res != null && res.isFinal()) {
                    finish(res.bestTranscription.formattedString)
                }
            }

            cont.invokeOnCancellation { finish(null) }
        }
    }
}
