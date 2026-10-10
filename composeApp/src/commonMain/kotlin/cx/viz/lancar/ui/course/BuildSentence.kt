package cx.viz.lancar.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarInk
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LancarSurface
import cx.viz.lancar.ui.theme.LocalAccentColor

/**
 * Answer slots on top (tap to send a tile back), shuffled pool below (tap to place).
 * [placed] holds indices into [tiles]; placed tiles disappear from the pool.
 */
@Composable
fun BuildSentence(
    prompt: String,
    tiles: List<String>,
    placed: List<Int>,
    enabled: Boolean,
    onPlace: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val slotShape = RoundedCornerShape(16.dp)
    Column {
        Text(prompt, style = MaterialTheme.typography.headlineSmall, color = LancarInk)
        Spacer(Modifier.height(18.dp))
        FlowRow(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .background(LancarSurface, slotShape)
                .border(1.5.dp, LancarBorder, slotShape)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (placed.isEmpty()) {
                Text(
                    "Ketuk kata di bawah · Tap the words below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LancarSecondaryText.copy(alpha = 0.7f),
                    modifier = Modifier.padding(6.dp),
                )
            }
            placed.forEach { i -> Tile(tiles[i], accent = true, enabled = enabled) { onRemove(i) } }
        }
        Spacer(Modifier.height(18.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tiles.indices.filter { it !in placed }.forEach { i ->
                Tile(tiles[i], accent = false, enabled = enabled) { onPlace(i) }
            }
        }
    }
}

@Composable
private fun Tile(label: String, accent: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val bg = if (accent) LocalAccentColor.current.copy(alpha = 0.12f) else LancarPanel
    val borderColor = if (accent) LocalAccentColor.current else LancarBorder
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        color = LancarInk,
        modifier = Modifier
            .background(bg, shape)
            .border(1.5.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
