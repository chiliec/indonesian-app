package cx.viz.lancar

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {

    // Bridges the ActivityResult callback back to the suspending caller in [requestMicPermission].
    private var pendingMicPermission: ((Boolean) -> Unit)? = null

    // Must be registered before the activity is STARTED — a member field satisfies that.
    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingMicPermission?.invoke(granted)
            pendingMicPermission = null
        }

    /** Shows the RECORD_AUDIO prompt if not already granted; suspends until the user decides. */
    private suspend fun requestMicPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) return true
        return suspendCancellableCoroutine { cont ->
            pendingMicPermission = { granted -> cont.resume(granted) }
            cont.invokeOnCancellation { pendingMicPermission = null }
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val db = LancarDatabase(DriverFactory(applicationContext).createDriver())
        val appModule = AppModule(
            content = ContentRepository(),
            progress = ProgressRepository(db),
            settings = SettingsRepository(db),
            audio = AndroidAudioPlayer(applicationContext),
            tts = AndroidSpeechSynthesizer(applicationContext),
            stt = AndroidSpeechRecognizer(applicationContext, ::requestMicPermission),
        )
        setContent { App(appModule) }
    }
}
