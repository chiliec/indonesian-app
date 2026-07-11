package cx.viz.lancar.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

class IosSpeechSynthesizer : SpeechSynthesizer {
    private val synth = AVSpeechSynthesizer()

    override suspend fun speak(text: String, lang: String) = withContext(Dispatchers.Main) {
        if (text.isBlank()) return@withContext
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage(lang)
        if (voice == null) return@withContext
        if (synth.isSpeaking()) synth.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = voice
        synth.speakUtterance(utterance)
    }
}
