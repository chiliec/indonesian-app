package cx.viz.lancar.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    DisposableEffect(darkIcons) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = darkIcons
        // Restore the app default (dark icons on the light cream UI) when leaving this screen.
        onDispose { controller.isAppearanceLightStatusBars = true }
    }
}
