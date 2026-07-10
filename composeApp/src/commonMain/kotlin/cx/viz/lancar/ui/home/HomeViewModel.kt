package cx.viz.lancar.ui.home

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

data class ModuleRow(val id: String, val title: String, val cardCount: Int, val masteryPct: Int)
data class HomeUiState(
    val modules: List<ModuleRow> = emptyList(),
    val loading: Boolean = true,
    val name: String = "",
    val dueCount: Int = 0,
)

class HomeViewModel(
    private val module: AppModule,
    dispatcher: CoroutineContext = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        scope.launch {
            val metas = module.content.modules()
            val rows = metas.map { m ->
                val ids = module.content.cards(m.id).map { it.id }
                ModuleRow(m.id, m.title, m.cardCount, module.progress.modulePercent(ids))
            }
            _state.value = HomeUiState(
                modules = rows,
                loading = false,
                name = module.settings.displayName().orEmpty(),
                dueCount = module.progress.countDue(),
            )
        }
    }

    fun dispose() { scope.cancel() }
}
