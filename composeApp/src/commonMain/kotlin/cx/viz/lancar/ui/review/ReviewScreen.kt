package cx.viz.lancar.ui.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.drill.QuizView

@Composable
fun ReviewScreen(
    appModule: AppModule,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    val vm = remember { ReviewViewModel(appModule) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    val state by vm.state.collectAsState()

    LaunchedEffect(state.finished) {
        if (state.finished) onFinish()
    }

    QuizView(
        state = state,
        onAnswer = { vm.answer(it) },
        onNext = { vm.next() },
        onPlayAudio = { vm.playAudio() },
        onBack = onBack,
    )
}
