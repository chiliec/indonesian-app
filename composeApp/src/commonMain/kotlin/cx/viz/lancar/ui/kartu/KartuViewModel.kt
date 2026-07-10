package cx.viz.lancar.ui.kartu

import cx.viz.lancar.data.MIXED_ID
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseModuleRow(val id: String, val title: String, val cardCount: Int)

data class KartuUiState(
    val modules: List<BrowseModuleRow> = emptyList(),
    val loading: Boolean = true,
)

class KartuViewModel(
    private val module: AppModule,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow(KartuUiState())
    val state: StateFlow<KartuUiState> = _state.asStateFlow()

    init {
        scope.launch {
            val rows = module.content.modules()
                .filter { it.id != MIXED_ID }
                .map { BrowseModuleRow(it.id, it.title, it.cardCount) }
            _state.value = KartuUiState(rows, loading = false)
        }
    }

    fun dispose() { scope.cancel() }
}
