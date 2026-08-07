package cx.viz.lancar.ui.drill.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarCorrectBg
import cx.viz.lancar.ui.theme.LancarCream
import cx.viz.lancar.ui.theme.LancarGreen
import cx.viz.lancar.ui.theme.LancarInk
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LancarSurface
import cx.viz.lancar.ui.theme.LancarWrongBg
import cx.viz.lancar.ui.theme.LancarWrongBorder
import cx.viz.lancar.ui.theme.LocalAccentColor

enum class OptState { IDLE, SELECTED, CORRECT, WRONG, DIMMED }

/** One multiple-choice answer with its A–D key badge. */
@Composable
fun OptionRow(
    label: String,
    key: String,
    state: OptState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalAccentColor.current
    val shape = RoundedCornerShape(18.dp)
    val dim = state == OptState.DIMMED
    fun fade(c: androidx.compose.ui.graphics.Color) = if (dim) c.copy(alpha = 0.55f) else c

    val bg by animateColorAsState(
        fade(
            when (state) {
                OptState.SELECTED -> LancarPanel
                OptState.CORRECT -> LancarCorrectBg
                OptState.WRONG -> LancarWrongBg
                else -> LancarSurface
            },
        ),
        tween(160),
        label = "optionBg",
    )
    val border by animateColorAsState(
        fade(
            when (state) {
                OptState.SELECTED -> accent
                OptState.CORRECT -> LancarGreen
                OptState.WRONG -> LancarWrongBorder
                else -> LancarBorder
            },
        ),
        tween(160),
        label = "optionBorder",
    )
    val badgeBg = when (state) {
        OptState.SELECTED -> accent
        OptState.CORRECT -> LancarGreen
        OptState.WRONG -> LancarWrongBorder
        else -> LancarInk.copy(alpha = 0.07f)
    }
    val badgeFg = when (state) {
        OptState.IDLE, OptState.DIMMED -> LancarSecondaryText
        else -> LancarCream
    }
    val badgeText = when (state) {
        OptState.CORRECT -> "✓"
        OptState.WRONG -> "✕"
        else -> key
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.5.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(fade(badgeBg)),
            contentAlignment = Alignment.Center,
        ) {
            Text(badgeText, style = MaterialTheme.typography.labelSmall, color = fade(badgeFg))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = fade(LancarInk),
        )
    }
}
