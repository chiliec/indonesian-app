package cx.viz.lancar.ui.drill

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.viz.lancar.domain.QuestionMode
import cx.viz.lancar.ui.theme.*

/** Stateless quiz UI shared by drill and review. All state + actions are passed in. */
@Composable
fun QuizView(
    state: DrillUiState,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onPlayAudio: () -> Unit,
    onBack: () -> Unit,
    onSpeak: () -> Unit = {},
) {
    val q = state.question ?: run {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            Alignment.Center,
        ) { CircularProgressIndicator(color = LocalAccentColor.current) }
        return
    }

    val kindLabel = when (q.mode) {
        QuestionMode.LISTEN -> "DENGARKAN · LISTEN"
        QuestionMode.TEXT -> "PILIH ARTINYA · PICK THE MEANING"
        QuestionMode.PRODUCE -> "PILIH KATANYA · PICK THE WORD"
    }

    val topPad = topContentPadding(12.dp)
    val bottomPad = screenBottomPadding(28.dp)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(PaddingValues(start = 24.dp, end = 24.dp, top = topPad, bottom = bottomPad)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "✕",
                style = MaterialTheme.typography.titleLarge,
                color = LancarSecondaryText,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 14.dp),
            )
            LinearProgressIndicator(
                progress = { (state.index + if (state.answered) 1 else 0).toFloat() / state.total },
                modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(99.dp)),
                color = LocalAccentColor.current,
                trackColor = LancarPanel,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                "${state.index + 1} / ${state.total}",
                style = MaterialTheme.typography.labelMedium,
                color = LancarSecondaryText,
                modifier = Modifier.padding(start = 14.dp),
            )
        }

        Spacer(Modifier.height(30.dp))
        Text(kindLabel, style = MaterialTheme.typography.labelMedium, color = LocalAccentColor.current, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))

        if (q.mode == QuestionMode.LISTEN) {
            AudioButton(onPlay = onPlayAudio)
        } else {
            Text(q.promptText, style = MaterialTheme.typography.headlineMedium)
            if (q.mode == QuestionMode.PRODUCE && state.sttAvailable && !state.answered) {
                Spacer(Modifier.height(16.dp))
                MicButton(listening = state.listening, onSpeak = onSpeak)
                state.speechHint?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = LancarSecondaryText)
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        q.options.forEachIndexed { i, opt ->
            OptionButton(
                label = opt,
                enabled = !state.answered,
                state = when {
                    !state.answered -> OptState.IDLE
                    i == q.correctIndex -> OptState.CORRECT
                    i == state.selected -> OptState.WRONG
                    else -> OptState.DIMMED
                },
                onClick = { onAnswer(i) },
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.weight(1f))

        if (state.answered) {
            val correct = state.selected == q.correctIndex
            Feedback(
                correct = correct,
                note = q.card.note,
                isLast = state.index == state.total - 1,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun AudioButton(onPlay: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LocalAccentColor.current)
            .clickable(onClick = onPlay)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔉  Putar audio", style = MaterialTheme.typography.labelLarge, color = LancarCream)
    }
}

@Composable
private fun MicButton(listening: Boolean, onSpeak: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LocalAccentColor.current)
            .clickable(enabled = !listening, onClick = onSpeak)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (listening) "🎤  Mendengarkan…" else "🎤  Ucapkan dalam bahasa Indonesia",
            style = MaterialTheme.typography.labelLarge,
            color = LancarCream,
        )
    }
}

private enum class OptState { IDLE, CORRECT, WRONG, DIMMED }

@Composable
private fun OptionButton(
    label: String,
    enabled: Boolean,
    state: OptState,
    onClick: () -> Unit,
) {
    val bg = when (state) {
        OptState.CORRECT -> LancarCorrectBg
        OptState.WRONG -> LancarWrongBg
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when (state) {
        OptState.CORRECT -> LancarGreen
        OptState.WRONG -> LancarWrongBorder
        else -> LancarBorder
    }
    val textColor = when (state) {
        OptState.CORRECT -> LancarCorrectText
        OptState.WRONG -> LancarWrongText
        OptState.DIMMED -> LancarInk.copy(alpha = 0.45f)
        OptState.IDLE -> LancarInk
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}

@Composable
private fun Feedback(correct: Boolean, note: String?, isLast: Boolean, onNext: () -> Unit) {
    val bg = if (correct) LancarCorrectBg else LancarWrongBg
    val fg = if (correct) LancarCorrectText else LancarWrongText
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(20.dp),
    ) {
        Text(
            if (correct) "Betul! 🎉" else "Hampir…",
            style = MaterialTheme.typography.titleMedium,
            color = fg,
        )
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(note, style = MaterialTheme.typography.bodyMedium, color = fg)
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(LancarInk)
                .clickable(onClick = onNext)
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isLast) "Selesai ✓" else "Lanjut →",
                style = MaterialTheme.typography.labelLarge,
                color = LancarCream,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
