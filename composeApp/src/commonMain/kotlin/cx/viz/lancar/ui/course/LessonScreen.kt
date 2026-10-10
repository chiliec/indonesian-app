package cx.viz.lancar.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cx.viz.lancar.domain.Step
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.drill.quiz.FeedbackSheet
import cx.viz.lancar.ui.theme.LancarCream
import cx.viz.lancar.ui.theme.LancarInk
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LocalAccentColor
import cx.viz.lancar.ui.theme.accentGradient
import cx.viz.lancar.ui.theme.screenBottomPadding
import cx.viz.lancar.ui.theme.topContentPadding

@Composable
fun LessonScreen(
    appModule: AppModule,
    lessonId: String,
    onClose: () -> Unit,
    onNextLesson: (String) -> Unit,
) {
    val vm = remember(lessonId) { LessonViewModel(appModule, lessonId) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    val state by vm.state.collectAsState()
    LessonView(
        state = state,
        onClose = onClose,
        onNextLesson = onNextLesson,
        onPlaceTile = vm::placeTile,
        onRemoveTile = vm::removeTile,
        onSelectOption = vm::selectOption,
        onTyped = vm::setTyped,
        onCheck = vm::check,
        onNext = vm::next,
        onSpeak = vm::speak,
        onRestart = vm::restart,
    )
}

@Composable
fun LessonView(
    state: LessonUiState,
    onClose: () -> Unit,
    onNextLesson: (String) -> Unit,
    onPlaceTile: (Int) -> Unit,
    onRemoveTile: (Int) -> Unit,
    onSelectOption: (Int) -> Unit,
    onTyped: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSpeak: (String) -> Unit,
    onRestart: () -> Unit,
) {
    val lesson = state.lesson
    val total = lesson?.steps?.size ?: 0
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 22.dp, top = topContentPadding(12.dp), bottom = screenBottomPadding(16.dp)),
        ) {
            Header(stepIndex = state.stepIndex, total = total, finished = state.finished, onClose = onClose)
            Spacer(Modifier.height(20.dp))
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = LocalAccentColor.current)
                }
                lesson == null -> Text(
                    "Pelajaran tidak ditemukan · Lesson not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LancarSecondaryText,
                )
                state.finished -> LessonCompleteView(
                    score = state.score,
                    total = state.exerciseCount,
                    hasNext = state.nextLessonId != null,
                    onRestart = onRestart,
                    onNext = { state.nextLessonId?.let(onNextLesson) ?: onClose() },
                )
                else -> {
                    val step = state.step ?: return@Column
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        StepBody(step, state, onPlaceTile, onRemoveTile, onSelectOption, onTyped, onCheck, onSpeak)
                        Spacer(Modifier.height(24.dp))
                    }
                    if (step is Step.Theory) {
                        PrimaryButton("Lanjut · Continue", enabled = true, onClick = onNext)
                    } else if (!state.checked) {
                        PrimaryButton("Periksa · Check", enabled = state.canCheck, onClick = onCheck)
                    }
                }
            }
        }
        if (state.checked && !state.finished) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                FeedbackSheet(
                    correct = state.correct == true,
                    word = state.promptText,
                    answer = state.expectedAnswer,
                    note = state.explain,
                    isLast = state.stepIndex == total - 1,
                    onNext = onNext,
                )
            }
        }
    }
}

@Composable
private fun StepBody(
    step: Step,
    state: LessonUiState,
    onPlaceTile: (Int) -> Unit,
    onRemoveTile: (Int) -> Unit,
    onSelectOption: (Int) -> Unit,
    onTyped: (String) -> Unit,
    onCheck: () -> Unit,
    onSpeak: (String) -> Unit,
) {
    val selectedOption = (state.selection as? Selection.Option)?.index
    when (step) {
        is Step.Theory -> TheorySlide(step, onSpeak)
        is Step.Choose -> ChooseTranslation(step.prompt, state.options, selectedOption, state.checked, state.correctIndex, onSelectOption)
        is Step.Fill -> FillBlank(step.text, state.options, selectedOption, state.checked, state.correctIndex, onSelectOption)
        is Step.Type -> TypeAnswer(
            prompt = step.prompt,
            text = (state.selection as? Selection.Typed)?.text.orEmpty(),
            checked = state.checked,
            onTextChange = onTyped,
            onDone = { if (state.canCheck) onCheck() },
        )
        is Step.Build -> BuildSentence(
            prompt = step.prompt,
            tiles = state.tiles,
            placed = (state.selection as? Selection.Tiles)?.placed.orEmpty(),
            enabled = !state.checked,
            onPlace = onPlaceTile,
            onRemove = onRemoveTile,
        )
    }
}

@Composable
private fun Header(stepIndex: Int, total: Int, finished: Boolean, onClose: () -> Unit) {
    val done = if (finished) total else stepIndex
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .background(LancarInk.copy(alpha = 0.06f), CircleShape)
                .clickable(onClickLabel = "Tutup · Close", onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", style = MaterialTheme.typography.labelLarge, color = LancarSecondaryText)
        }
        Spacer(Modifier.width(14.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.weight(1f).height(9.dp),
            color = LocalAccentColor.current,
            trackColor = LancarPanel,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            "${(stepIndex + 1).coerceAtMost(total)} / $total",
            style = MaterialTheme.typography.labelMedium,
            color = LancarSecondaryText,
        )
    }
}

@Composable
fun PrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(99.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .then(if (enabled) Modifier.background(accentGradient(), shape) else Modifier.background(LancarPanel, shape))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) LancarCream else LancarSecondaryText.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun LessonCompleteView(score: Int, total: Int, hasNext: Boolean, onRestart: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🎉", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text("Pelajaran selesai!", style = MaterialTheme.typography.headlineSmall, color = LancarInk)
        Spacer(Modifier.height(6.dp))
        Text("$score / $total benar", style = MaterialTheme.typography.bodyLarge, color = LancarSecondaryText)
        Spacer(Modifier.height(32.dp))
        PrimaryButton(if (hasNext) "Lanjut ke pelajaran berikutnya" else "Selesai ✓", enabled = true, onClick = onNext)
        Spacer(Modifier.height(10.dp))
        Text(
            "Ulangi",
            style = MaterialTheme.typography.labelLarge,
            color = LancarSecondaryText,
            modifier = Modifier.clickable(onClick = onRestart).padding(12.dp),
        )
    }
}
