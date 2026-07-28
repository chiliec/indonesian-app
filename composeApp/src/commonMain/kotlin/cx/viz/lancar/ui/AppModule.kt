package cx.viz.lancar.ui

import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.domain.QuestionFactory
import cx.viz.lancar.platform.AudioPlayer
import cx.viz.lancar.platform.NoopSpeechRecognizer
import cx.viz.lancar.platform.NoopSpeechSynthesizer
import cx.viz.lancar.platform.SpeechRecognizer
import cx.viz.lancar.platform.SpeechSynthesizer
import cx.viz.lancar.ui.theme.Accent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppModule(
    val content: ContentRepository,
    val progress: ProgressRepository,
    val settings: SettingsRepository,
    val audio: AudioPlayer,
    val tts: SpeechSynthesizer = NoopSpeechSynthesizer(),
    val stt: SpeechRecognizer = NoopSpeechRecognizer(),
    val questionFactory: QuestionFactory = QuestionFactory(),
) {

    private val _accent = MutableStateFlow(Accent.fromName(settings.accentName()))
    val accent: StateFlow<Accent> = _accent.asStateFlow()

    fun setAccent(a: Accent) {
        settings.setAccentName(a.name)
        _accent.value = a
    }
}
