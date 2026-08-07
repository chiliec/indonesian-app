package cx.viz.lancar.ui.drill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.viz.lancar.domain.QuestionMode
import cx.viz.lancar.ui.drill.quiz.AudioPanel
import cx.viz.lancar.ui.drill.quiz.FeedbackSheet
import cx.viz.lancar.ui.drill.quiz.OptState
import cx.viz.lancar.ui.drill.quiz.OptionRow
import cx.viz.lancar.ui.theme.*

private const val OPTION_KEYS = "ABCD"

/** Stateless quiz UI shared by drill and review. All state + actions are passed in. */
@Composable
fun QuizView(
    state: DrillUiState,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onPlayAudio: (slow: Boolean) -> Unit,
    onBack: () -> Unit,
    onSpeak: () -> Unit = {},
    onToggleWord: () -> Unit = {},
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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = topContentPadding(12.dp),
                    bottom = screenBottomPadding(28.dp),
                ),
        ) {
            Header(state, onBack)

            Spacer(Modifier.height(30.dp))
            Text(
                kindLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
                color = LocalAccentColor.current,
            )
            Spacer(Modifier.height(10.dp))

            if (q.mode == QuestionMode.LISTEN) {
                AudioPanel(
                    playing = state.playing,
                    word = q.card.indonesian,
                    revealed = state.revealText,
                    onPlay = onPlayAudio,
                    onToggleWord = onToggleWord,
                )
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
                OptionRow(
                    label = opt,
                    key = OPTION_KEYS[i].toString(),
                    state = when {
                        !state.answered -> if (state.selected == i) OptState.SELECTED else OptState.IDLE
                        i == q.correctIndex -> OptState.CORRECT
                        i == state.selected -> OptState.WRONG
                        else -> OptState.DIMMED
                    },
                    enabled = !state.answered,
                    onClick = { onSelect(i) },
                )
                Spacer(Modifier.height(12.dp))
            }

            if (!state.answered) {
                Spacer(Modifier.height(10.dp))
                CheckButton(enabled = state.selected != null, onClick = onCheck)
            }
        }

        AnimatedVisibility(
            visible = state.answered,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = fadeOut(),
        ) {
            FeedbackSheet(
                correct = state.selected == q.correctIndex,
                word = q.card.indonesian,
                answer = q.card.english,
                note = q.card.note,
                isLast = state.index == state.total - 1,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun Header(state: DrillUiState, onBack: () -> Unit) {
    val target = (state.index + if (state.answered) 1 else 0).toFloat() /
        state.total.coerceAtLeast(1)
    val pct by animateFloatAsState(target, tween(450), label = "quizProgress")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(LancarInk.copy(alpha = 0.06f))
                .clickable(onClickLabel = "Tutup · Close", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", style = MaterialTheme.typography.labelLarge, color = LancarSecondaryText)
        }
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier
                .weight(1f)
                .height(9.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(LancarPanel),
        ) {
            // fillMaxWidth rejects 0f, so an empty bar renders as an invisible sliver.
            Box(
                Modifier
                    .fillMaxWidth(pct.coerceIn(0.0001f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(accentGradient()),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            "${state.index + 1} / ${state.total}",
            style = MaterialTheme.typography.labelMedium,
            color = LancarSecondaryText,
        )
    }
}

@Composable
private fun CheckButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(99.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .then(if (enabled) Modifier.background(accentGradient()) else Modifier.background(LancarPanel))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Periksa · Check",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) LancarCream else LancarSecondaryText.copy(alpha = 0.6f),
        )
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
