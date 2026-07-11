package cx.viz.lancar

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.DriverFactory
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.platform.IosAudioPlayer
import cx.viz.lancar.platform.IosSpeechRecognizer
import cx.viz.lancar.platform.IosSpeechSynthesizer
import cx.viz.lancar.ui.App
import cx.viz.lancar.ui.AppModule

fun MainViewController() = ComposeUIViewController {
    val db = remember { LancarDatabase(DriverFactory().createDriver()) }
    val appModule = remember {
        AppModule(
            content = ContentRepository(),
            progress = ProgressRepository(db),
            settings = SettingsRepository(db),
            audio = IosAudioPlayer(),
            tts = IosSpeechSynthesizer(),
            stt = IosSpeechRecognizer(),
        )
    }
    App(appModule)
}
