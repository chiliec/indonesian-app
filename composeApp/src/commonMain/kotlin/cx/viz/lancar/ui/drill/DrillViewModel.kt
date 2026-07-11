package cx.viz.lancar.ui.drill

import cx.viz.lancar.domain.Card
import cx.viz.lancar.domain.MasteryCalculator
import cx.viz.lancar.domain.PronunciationMatcher
import cx.viz.lancar.domain.Question
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SESSION_SIZE = 12

data class DrillUiState(
    val index: Int = 0,
    val total: Int = SESSION_SIZE,
    val question: Question? = null,
    val selected: Int? = null,
    val answered: Boolean = false,
    val correctCount: Int = 0,
    val finished: Boolean = false,
    val newlyMastered: Int = 0,
    val sttAvailable: Boolean = false,
    val listening: Boolean = false,
    val speechHint: String? = null,
)

class DrillViewModel(
    private val module: AppModule,
    private val moduleId: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(DrillUiState())
    val state: StateFlow<DrillUiState> = _state.asStateFlow()

    private lateinit var pool: List<Card>
    private lateinit var queue: List<Card>
    private var sttAvailable = false

    init {
        scope.launch {
            pool = module.content.cards(moduleId)
            val prog = withContext(Dispatchers.Default) {
                module.progress.forCards(pool.map { it.id })
            }
            queue = pool.sortedBy { c ->
                val p = prog[c.id]
                when {
                    p == null -> 0                        // unseen first
                    !MasteryCalculator.isMastered(p) -> 1 // then wrong/seen
                    else -> 2                             // mastered last
                }
            }.take(SESSION_SIZE)
            sttAvailable = module.stt.isAvailable()
            emitQuestion(0, 0)
        }
    }

    private suspend fun emitQuestion(index: Int, correctCount: Int) {
        if (index >= queue.size) {
            _state.value = _state.value.copy(finished = true, correctCount = correctCount)
            return
        }
        val card = queue[index]
        val prog = withContext(Dispatchers.Default) {
            module.progress.forCards(listOf(card.id))
        }
        val mastered = MasteryCalculator.isMastered(prog[card.id])
        val q = module.questionFactory.build(card, pool, mastered)
        _state.value = DrillUiState(
            index = index, total = queue.size, question = q,
            correctCount = correctCount, newlyMastered = _state.value.newlyMastered,
            sttAvailable = sttAvailable,
        )
    }

    fun answer(optionIndex: Int) {
        val s = _state.value
        val q = s.question ?: return
        if (s.answered) return
        val correct = optionIndex == q.correctIndex
        val wasMastered = MasteryCalculator.isMastered(
            module.progress.forCards(listOf(q.card.id))[q.card.id]
        )
        module.progress.recordAnswer(q.card.id, correct)
        val nowMastered = correct && !wasMastered
        _state.value = s.copy(
            selected = optionIndex,
            answered = true,
            correctCount = s.correctCount + if (correct) 1 else 0,
            newlyMastered = s.newlyMastered + if (nowMastered) 1 else 0,
        )
    }

    fun onSpeak() {
        val s = _state.value
        val q = s.question ?: return
        if (s.answered || s.listening) return
        scope.launch {
            _state.value = _state.value.copy(listening = true, speechHint = null)
            val transcript = module.stt.recognize()
            val ok = transcript != null && PronunciationMatcher.matches(transcript, q.card.indonesian)
            if (ok) {
                _state.value = _state.value.copy(listening = false)
                answer(q.correctIndex)
            } else {
                _state.value = _state.value.copy(
                    listening = false,
                    speechHint = "Belum tepat — coba lagi atau ketuk",
                )
            }
        }
    }

    fun next() {
        val s = _state.value
        scope.launch {
            emitQuestion(s.index + 1, s.correctCount)
        }
    }

    fun playAudio() {
        val name = _state.value.question?.audio ?: return
        scope.launch { module.audio.play(name) }
    }

    fun dispose() { scope.cancel() }
}
