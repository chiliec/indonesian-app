package cx.viz.lancar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.DriverFactory
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.platform.AndroidAudioPlayer
import cx.viz.lancar.platform.AndroidSpeechRecognizer
import cx.viz.lancar.platform.AndroidSpeechSynthesizer
import cx.viz.lancar.ui.App
import cx.viz.lancar.ui.AppModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = LancarDatabase(DriverFactory(applicationContext).createDriver())
        val appModule = AppModule(
            content = ContentRepository(),
            progress = ProgressRepository(db),
            settings = SettingsRepository(db),
            audio = AndroidAudioPlayer(applicationContext),
            tts = AndroidSpeechSynthesizer(applicationContext),
            stt = AndroidSpeechRecognizer(applicationContext),
        )
        setContent { App(appModule) }
    }
}
