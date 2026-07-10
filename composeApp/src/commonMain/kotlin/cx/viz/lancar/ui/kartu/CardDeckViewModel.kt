package cx.viz.lancar.ui.kartu

import cx.viz.lancar.domain.Card
import cx.viz.lancar.domain.deckOrder
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
import kotlin.random.Random

data class CardDeckUiState(
    val cards: List<Card> = emptyList(),
    val loading: Boolean = true,
    val shuffled: Boolean = false,
)

class CardDeckViewModel(
    private val module: AppModule,
    private val moduleId: String,
    private val rng: Random = Random.Default,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow(CardDeckUiState())
    val state: StateFlow<CardDeckUiState> = _state.asStateFlow()

    private var source: List<Card> = emptyList()

    init {
        scope.launch {
            source = module.content.cards(moduleId)
            _state.value = CardDeckUiState(
                cards = deckOrder(source, shuffled = false, rng = rng),
                loading = false,
                shuffled = false,
            )
        }
    }

    fun toggleShuffle() {
        val next = !_state.value.shuffled
        _state.value = _state.value.copy(
            cards = deckOrder(source, shuffled = next, rng = rng),
            shuffled = next,
        )
    }

    fun playAudio(fileName: String) {
        scope.launch { module.audio.play(fileName) }
    }

    fun dispose() { scope.cancel() }
}
