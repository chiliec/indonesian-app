package cx.viz.lancar.ui.drill.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cx.viz.lancar.ui.theme.LancarCorrectBg
import cx.viz.lancar.ui.theme.LancarCorrectText
import cx.viz.lancar.ui.theme.LancarCream
import cx.viz.lancar.ui.theme.LancarGreen
import cx.viz.lancar.ui.theme.LancarWrongBg
import cx.viz.lancar.ui.theme.LancarWrongBorder
import cx.viz.lancar.ui.theme.LancarWrongText
import cx.viz.lancar.ui.theme.accentGradient
import cx.viz.lancar.ui.theme.gradientOf

/** Slide-up result sheet shown after the learner checks an answer. */
@Composable
fun FeedbackSheet(
    correct: Boolean,
    word: String,
    answer: String,
    note: String?,
    isLast: Boolean,
    onNext: () -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    val fg = if (correct) LancarCorrectText else LancarWrongText
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (correct) LancarCorrectBg else LancarWrongBg, shape)
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (correct) LancarGreen else LancarWrongBorder),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (correct) "✓" else "✕",
                    style = MaterialTheme.typography.labelMedium,
                    color = LancarCream,
                )
            }
            Text(
                if (correct) "Benar! · Correct" else "Kurang tepat · Not quite",
                style = MaterialTheme.typography.titleMedium,
                color = fg,
            )
        }
        Text("“$word” — $answer", style = MaterialTheme.typography.bodyMedium, color = fg)
        if (!note.isNullOrBlank()) {
            Text(note, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.8f))
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (correct) gradientOf(LancarGreen) else accentGradient())
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isLast) "Selesai ✓" else "Lanjut · Continue",
                style = MaterialTheme.typography.labelLarge,
                color = LancarCream,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
