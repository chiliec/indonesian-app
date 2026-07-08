package com.axveer.lancar.ui.drill

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.axveer.lancar.domain.QuestionMode
import com.axveer.lancar.ui.AppModule

@OptIn(ExperimentalMaterial3Api::class)
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

    val q = state.question ?: run {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("${state.index + 1} / ${state.total}") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Close") } })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            when (q.mode) {
                QuestionMode.LISTEN -> Button(onClick = { vm.playAudio() }) { Text("▶ Play audio") }
                else -> Text(q.promptText, style = MaterialTheme.typography.headlineSmall)
            }
            if (q.mode == QuestionMode.LISTEN) {
                Spacer(Modifier.height(8.dp)); Text(q.promptText)
            }
            Spacer(Modifier.height(24.dp))
            q.options.forEachIndexed { i, opt ->
                val color = when {
                    !state.answered -> MaterialTheme.colorScheme.surfaceVariant
                    i == q.correctIndex -> Color(0xFF2E7D32)
                    i == state.selected -> Color(0xFFC62828)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Button(
                    onClick = { vm.answer(i) },
                    enabled = !state.answered || true,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) { Text(opt) }
            }
            if (state.answered) {
                Spacer(Modifier.height(16.dp))
                q.card.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth()) { Text("Next") }
            }
        }
    }
}
