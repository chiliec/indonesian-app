package cx.viz.lancar.ui.theme

import androidx.compose.runtime.Composable

/**
 * Controls system status-bar icon contrast for the current screen.
 * [darkIcons] = true → dark icons (light background, the app default);
 * false → light icons (dark background). The app default (dark) is restored when
 * this composable leaves the composition. No-op on iOS.
 */
@Composable
expect fun StatusBarIcons(darkIcons: Boolean)
