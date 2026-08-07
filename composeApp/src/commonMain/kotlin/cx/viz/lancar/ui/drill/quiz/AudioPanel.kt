package cx.viz.lancar.ui.drill.quiz

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarCream
import cx.viz.lancar.ui.theme.LancarInk
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LancarSurface
import cx.viz.lancar.ui.theme.LocalAccentColor
import cx.viz.lancar.ui.theme.accentGradient

private val BarHeights = listOf(16.dp, 30.dp, 42.dp, 30.dp, 16.dp)

/** Play tile + hint + Pelan/Lihat-kata chips + the dashed reveal chip, for LISTEN questions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioPanel(
    playing: Boolean,
    word: String,
    revealed: Boolean,
    onPlay: (slow: Boolean) -> Unit,
    onToggleWord: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EqualizerTile(playing = playing, onClick = { onPlay(false) })
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    if (playing) "Memutar…" else "Ketuk untuk memutar",
                    style = MaterialTheme.typography.titleSmall,
                    color = LancarInk,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuizChip("Pelan · Slow") { onPlay(true) }
                    QuizChip("Lihat kata · Show word", onClick = onToggleWord)
                }
            }
        }
        if (revealed) {
            Spacer(Modifier.height(14.dp))
            RevealedWord(word)
        }
    }
}

@Composable
private fun EqualizerTile(playing: Boolean, onClick: () -> Unit) {
    // Gated on `playing`: a permanently running infinite transition would stop Compose UI
    // tests from ever reaching idle.
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        Modifier
            .size(86.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(accentGradient())
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Putar audio · Play audio"
                role = Role.Button
            },
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarHeights.forEachIndexed { i, h ->
            val scale = if (playing) {
                transition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(i * 120),
                    ),
                    label = "bar$i",
                ).value
            } else {
                0.35f
            }
            Box(
                Modifier
                    .width(6.dp)
                    .height(h)
                    .graphicsLayer { scaleY = scale }
                    .clip(RoundedCornerShape(99.dp))
                    .background(LancarCream),
            )
        }
    }
}

@Composable
private fun QuizChip(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(99.dp)
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = LancarSecondaryText,
        modifier = Modifier
            .clip(shape)
            .background(LancarSurface)
            .border(1.5.dp, LancarBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    )
}

@Composable
private fun RevealedWord(word: String) {
    val accent = LocalAccentColor.current
    val shape = RoundedCornerShape(14.dp)
    Text(
        word,
        style = MaterialTheme.typography.titleMedium,
        color = accent,
        modifier = Modifier
            .clip(shape)
            .background(LancarSurface)
            .drawBehind {
                drawRoundRect(
                    color = accent.copy(alpha = 0.45f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f)),
                    ),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                )
            }
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
