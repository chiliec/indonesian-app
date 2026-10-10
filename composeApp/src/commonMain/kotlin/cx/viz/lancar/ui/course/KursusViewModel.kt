package cx.viz.lancar.ui.course

import cx.viz.lancar.domain.unlockedLessonIds
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

sealed class RowState {
    data object Locked : RowState()
    data object Current : RowState()
    data class Done(val score: Int, val total: Int) : RowState()
}

data class LessonRow(val id: String, val index: Int, val title: String, val subtitle: String, val state: RowState)

data class KursusUiState(
    val rows: List<LessonRow> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val loading: Boolean = true,
)

class KursusViewModel(
    private val module: AppModule,
    dispatcher: CoroutineContext = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _state = MutableStateFlow(KursusUiState())
    val state: StateFlow<KursusUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        scope.launch {
            val lessons = module.content.course()
            val completed = module.course.completed()
            val open = unlockedLessonIds(lessons.map { it.id }, completed.keys)
            val rows = lessons.mapIndexed { i, l ->
                val state = when {
                    l.id in completed -> RowState.Done(completed.getValue(l.id), l.exerciseCount)
                    l.id in open -> RowState.Current
                    else -> RowState.Locked
                }
                LessonRow(l.id, i + 1, l.title, l.subtitle, state)
            }
            _state.value = KursusUiState(rows, completed.size, lessons.size, loading = false)
        }
    }

    fun dispose() { scope.cancel() }
}
