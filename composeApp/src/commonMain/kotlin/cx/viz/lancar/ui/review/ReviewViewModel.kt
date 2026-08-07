package cx.viz.lancar.ui.review

import cx.viz.lancar.data.MIXED_ID
import cx.viz.lancar.domain.Card
import cx.viz.lancar.domain.PronunciationMatcher
import cx.viz.lancar.domain.QuestionMode
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.drill.DrillUiState
import cx.viz.lancar.ui.drill.SLOW_RATE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

const val REVIEW_SIZE = 12

class ReviewViewModel(
    private val module: AppModule,
    dispatcher: CoroutineContext = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val showListenText = module.settings.showListenText()
    private val autoPlay = module.settings.autoPlayAudio()
    private val _state = MutableStateFlow(DrillUiState(revealText = showListenText))
    val state: StateFlow<DrillUiState> = _state.asStateFlow()

    private var pool: List<Card> = emptyList()
    private var queue: List<Card> = emptyList()
    private var sttAvailable = false
    private var playJob: Job? = null

    init {
        scope.launch {
            pool = module.content.cards(MIXED_ID) // all cards, for distractors + lookup
            val byId = pool.associateBy { it.id }
            val dueIds = module.progress.dueCardIds(REVIEW_SIZE)
            queue = dueIds.mapNotNull { byId[it] }
            sttAvailable = module.stt.isAvailable()
            emitQuestion(0, 0)
        }
    }

    private fun emitQuestion(index: Int, correctCount: Int) {
        if (index >= queue.size) {
            _state.value = _state.value.copy(finished = true, total = queue.size, correctCount = correctCount)
            return
        }
        val card = queue[index]
        // Every due card is already mastered, so isMastered = true.
        val q = module.questionFactory.build(card, pool, isMastered = true)
        _state.value = DrillUiState(
            index = index, total = queue.size, question = q, correctCount = correctCount,
            sttAvailable = sttAvailable, revealText = showListenText,
        )
        if (autoPlay && q.mode == QuestionMode.LISTEN) {
            playAudio()
        }
    }

    fun select(optionIndex: Int) {
        val s = _state.value
        if (s.question == null || s.answered) return
        _state.value = s.copy(selected = optionIndex)
    }

    fun check() {
        val s = _state.value
        val q = s.question ?: return
        val sel = s.selected ?: return
        if (s.answered) return
        val correct = sel == q.correctIndex
        module.progress.recordReview(q.card.id, correct)
        _state.value = s.copy(
            answered = true,
            correctCount = s.correctCount + if (correct) 1 else 0,
        )
    }

    /** One-step answer, used by the STT path where recognition already committed the choice. */
    fun answer(optionIndex: Int) {
        select(optionIndex)
        check()
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
        scope.launch { emitQuestion(s.index + 1, s.correctCount) }
    }

    fun toggleWord() {
        _state.value = _state.value.copy(revealText = !_state.value.revealText)
    }

    fun playAudio(slow: Boolean = false) {
        val name = _state.value.question?.audio ?: return
        playJob?.cancel()
        playJob = scope.launch {
            _state.value = _state.value.copy(playing = true)
            val ms = try {
                module.audio.play(name, if (slow) SLOW_RATE else 1f)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                0L
            }
            delay(ms)
            _state.value = _state.value.copy(playing = false)
        }
    }

    fun dispose() { scope.cancel() }
}
