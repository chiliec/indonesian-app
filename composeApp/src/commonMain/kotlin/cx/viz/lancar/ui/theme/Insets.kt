package cx.viz.lancar.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vertical space the floating pill tab bar occupies above the system nav bar:
 * pill height (~74dp) + its 16dp float gap + a small clearance. Screens that render
 * *under* the pill reserve this at the bottom so the last item is not hidden.
 */
val PillReservedHeight: Dp = 96.dp

/** Real status-bar / notch inset plus the screen's own top margin. */
@Composable
fun topContentPadding(extra: Dp): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + extra

/** Bottom padding for the four tab screens: nav-bar inset + room for the floating pill. */
@Composable
fun tabScreenBottomPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + PillReservedHeight

/** Bottom padding for full-screen (non-tab) screens: nav-bar inset + the screen's own margin. */
@Composable
fun screenBottomPadding(extra: Dp): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + extra
