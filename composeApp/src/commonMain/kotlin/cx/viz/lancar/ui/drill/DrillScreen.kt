package cx.viz.lancar.ui.drill

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cx.viz.lancar.ui.AppModule

@Composable
fun DrillScreen(
    appModule: AppModule,
    moduleId: String,
    onFinish: (correct: Int, total: Int, newlyMastered: Int) -> Unit,
    onBack: () -> Unit,
) {
    val vm = remember(moduleId) { DrillViewModel(appModule, moduleId) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    val state by vm.state.collectAsState()

    LaunchedEffect(state.finished) {
        if (state.finished) onFinish(state.correctCount, state.total, state.newlyMastered)
    }

    QuizView(
        state = state,
        onAnswer = { vm.answer(it) },
        onNext = { vm.next() },
        onPlayAudio = { vm.playAudio() },
        onBack = onBack,
        onSpeak = { vm.onSpeak() },
        onRevealWord = { vm.revealWord() },
    )
}
