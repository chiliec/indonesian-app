package cx.viz.lancar.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// The three accents offered by the design handoff (Theme.kt palette + blue alt).
val LancarBlue = Color(0xFF31547E)

enum class Accent(val color: Color) {
    TERRACOTTA(LancarTerracotta),
    GREEN(LancarGreen),
    BLUE(LancarBlue);

    companion object {
        fun fromName(name: String?): Accent = entries.firstOrNull { it.name == name } ?: TERRACOTTA
    }
}

/** 15% toward black — the darker stop of the button/tile gradients in the quiz screen. */
fun darken(c: Color): Color = lerp(c, Color.Black, 0.15f)

/** Vertical gradient used by the audio tile, progress fill, Check and Continue buttons. */
fun gradientOf(c: Color): Brush = Brush.verticalGradient(listOf(c, darken(c)))

@Composable
fun accentGradient(): Brush = gradientOf(LocalAccentColor.current)
