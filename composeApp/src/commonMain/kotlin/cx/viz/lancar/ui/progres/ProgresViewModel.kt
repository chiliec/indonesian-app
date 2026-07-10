package cx.viz.lancar.ui.progres

import cx.viz.lancar.data.MIXED_ID
import cx.viz.lancar.domain.BoxRow
import cx.viz.lancar.domain.ProgresStats
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

data class ProgresUiState(
    val loading: Boolean = true,
    val mastered: Int = 0,
    val total: Int = 0,
    val masteryPct: Int = 0,
    val accuracyPct: Int? = null,
    val seen: Int = 0,
    val reviewDeck: Int = 0,
    val boxRows: List<BoxRow> = emptyList(),
    val dueToday: Int = 0,
    val dueThisWeek: Int = 0,
)

class ProgresViewModel(
    private val module: AppModule,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow(ProgresUiState())
    val state: StateFlow<ProgresUiState> = _state.asStateFlow()

    init {
        scope.launch {
            val total = module.content.modules()
                .filter { it.id != MIXED_ID }
                .sumOf { it.cardCount }
            val p = module.progress
            val mastered = p.masteredCount()
            val totals = p.totals()
            _state.value = ProgresUiState(
                loading = false,
                mastered = mastered,
                total = total,
                masteryPct = ProgresStats.masteryPct(mastered, total),
                accuracyPct = ProgresStats.accuracyPct(totals.correct, totals.wrong),
                seen = p.seenCount(),
                reviewDeck = p.reviewDeckCount(),
                boxRows = ProgresStats.boxRows(p.boxCounts()),
                dueToday = p.countDue(),
                dueThisWeek = p.countDueWithin(7),
            )
        }
    }

    fun dispose() { scope.cancel() }
}
