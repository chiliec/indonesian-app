package com.axveer.lancar.ui.drill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axveer.lancar.domain.Card
import com.axveer.lancar.domain.MasteryCalculator
import com.axveer.lancar.domain.Question
import com.axveer.lancar.ui.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
)

class DrillViewModel(
    private val module: AppModule,
    private val moduleId: String,
) : ViewModel() {
    private val _state = MutableStateFlow(DrillUiState())
    val state: StateFlow<DrillUiState> = _state.asStateFlow()

    private lateinit var pool: List<Card>
    private lateinit var queue: List<Card>

    init {
        viewModelScope.launch {
            pool = module.content.cards(moduleId)
            val prog = module.progress.forCards(pool.map { it.id })
            queue = pool.sortedBy { c ->
                val p = prog[c.id]
                when {
                    p == null -> 0                        // unseen first
                    !MasteryCalculator.isMastered(p) -> 1 // then wrong/seen
                    else -> 2                             // mastered last
                }
            }.take(SESSION_SIZE)
            emitQuestion(0, 0)
        }
    }

    private fun emitQuestion(index: Int, correctCount: Int) {
        if (index >= queue.size) {
            _state.value = _state.value.copy(finished = true, correctCount = correctCount)
            return
        }
        val card = queue[index]
        val mastered = MasteryCalculator.isMastered(
            module.progress.forCards(listOf(card.id))[card.id]
        )
        val q = module.questionFactory.build(card, pool, mastered)
        _state.value = DrillUiState(
            index = index, total = queue.size, question = q,
            correctCount = correctCount, newlyMastered = _state.value.newlyMastered,
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

    fun next() {
        val s = _state.value
        emitQuestion(s.index + 1, s.correctCount)
    }

    fun playAudio() {
        val name = _state.value.question?.audio ?: return
        viewModelScope.launch { module.audio.play(name) }
    }
}
