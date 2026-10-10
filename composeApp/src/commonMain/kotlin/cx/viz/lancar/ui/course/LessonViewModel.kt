package cx.viz.lancar.ui.course

import cx.viz.lancar.domain.AnswerChecker
import cx.viz.lancar.domain.Lesson
import cx.viz.lancar.domain.Step
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
import kotlin.random.Random

sealed class Selection {
    /** Indices into [LessonUiState.tiles], in the order the learner placed them. */
    data class Tiles(val placed: List<Int>) : Selection()
    /** Index into [LessonUiState.options]. */
    data class Option(val index: Int) : Selection()
    data class Typed(val text: String) : Selection()
}

data class LessonUiState(
    val lesson: Lesson? = null,
    val loading: Boolean = true,
    val stepIndex: Int = 0,
    val tiles: List<String> = emptyList(),     // shuffled answer + extra (Build)
    val options: List<String> = emptyList(),   // shuffled options (Fill / Choose)
    val correctIndex: Int = -1,                // index into [options]
    val selection: Selection? = null,
    val checked: Boolean = false,
    val correct: Boolean? = null,
    val score: Int = 0,
    val exerciseCount: Int = 0,
    val finished: Boolean = false,
    val nextLessonId: String? = null,
) {
    val step: Step? get() = lesson?.steps?.getOrNull(stepIndex)

    val canCheck: Boolean get() = !checked && when (val s = selection) {
        is Selection.Tiles -> s.placed.isNotEmpty()
        is Selection.Option -> true
        is Selection.Typed -> s.text.isNotBlank()
        null -> false
    }

    /** The answer the learner should have given, shown in the feedback sheet. */
    val expectedAnswer: String get() = when (val s = step) {
        is Step.Build -> s.answer.joinToString(" ")
        is Step.Fill -> s.text.replace("___", s.options[s.correct])
        is Step.Choose -> s.options[s.correct]
        is Step.Type -> s.accept.first()
        is Step.Theory, null -> ""
    }

    /** Prompt text for the feedback sheet's first line. */
    val promptText: String get() = when (val s = step) {
        is Step.Build -> s.prompt
        is Step.Fill -> s.text
        is Step.Choose -> s.prompt
        is Step.Type -> s.prompt
        is Step.Theory, null -> ""
    }

    val explain: String? get() = when (val s = step) {
        is Step.Build -> s.explain
        is Step.Fill -> s.explain
        is Step.Choose -> s.explain
        is Step.Type -> s.explain
        is Step.Theory, null -> null
    }
}

class LessonViewModel(
    private val module: AppModule,
    private val lessonId: String,
    private val random: Random = Random.Default,
    dispatcher: CoroutineContext = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    init {
        scope.launch {
            val course = module.content.course()
            val idx = course.indexOfFirst { it.id == lessonId }
            val lesson = course.getOrNull(idx)
            val base = LessonUiState(
                lesson = lesson,
                loading = false,
                exerciseCount = lesson?.exerciseCount ?: 0,
                nextLessonId = course.getOrNull(idx + 1)?.id,
            )
            _state.value = if (lesson == null) base else enter(base, 0)
        }
    }

    /** Fresh per-step state: shuffles tiles/options through the injected Random. */
    private fun enter(s: LessonUiState, index: Int): LessonUiState {
        val clean = s.copy(
            stepIndex = index, selection = null, checked = false, correct = null,
            tiles = emptyList(), options = emptyList(), correctIndex = -1,
        )
        return when (val step = s.lesson!!.steps[index]) {
            is Step.Theory -> clean
            is Step.Build -> clean.copy(
                tiles = (step.answer + step.extra).shuffled(random),
                selection = Selection.Tiles(emptyList()),
            )
            is Step.Fill -> clean.withShuffled(step.options, step.correct)
            is Step.Choose -> clean.withShuffled(step.options, step.correct)
            is Step.Type -> clean.copy(selection = Selection.Typed(""))
        }
    }

    private fun LessonUiState.withShuffled(options: List<String>, correct: Int): LessonUiState {
        val order = options.indices.shuffled(random)
        return copy(options = order.map(options::get), correctIndex = order.indexOf(correct))
    }

    fun placeTile(poolIndex: Int) {
        val s = _state.value
        val sel = s.selection as? Selection.Tiles ?: return
        if (s.checked || poolIndex in sel.placed || poolIndex !in s.tiles.indices) return
        _state.value = s.copy(selection = Selection.Tiles(sel.placed + poolIndex))
    }

    fun removeTile(poolIndex: Int) {
        val s = _state.value
        val sel = s.selection as? Selection.Tiles ?: return
        if (s.checked) return
        _state.value = s.copy(selection = Selection.Tiles(sel.placed - poolIndex))
    }

    fun selectOption(index: Int) {
        val s = _state.value
        if (s.checked || index !in s.options.indices) return
        _state.value = s.copy(selection = Selection.Option(index))
    }

    fun setTyped(text: String) {
        val s = _state.value
        if (s.checked || s.step !is Step.Type) return
        _state.value = s.copy(selection = Selection.Typed(text))
    }

    fun check() {
        val s = _state.value
        if (!s.canCheck) return
        val ok = when (val step = s.step) {
            is Step.Build -> AnswerChecker.checkBuild((s.selection as Selection.Tiles).placed.map(s.tiles::get), step.answer)
            is Step.Fill, is Step.Choose -> (s.selection as Selection.Option).index == s.correctIndex
            is Step.Type -> AnswerChecker.checkType((s.selection as Selection.Typed).text, step.accept)
            is Step.Theory, null -> return
        }
        _state.value = s.copy(checked = true, correct = ok, score = s.score + if (ok) 1 else 0)
    }

    fun next() {
        val s = _state.value
        val lesson = s.lesson ?: return
        if (s.finished || (s.step !is Step.Theory && !s.checked)) return
        if (s.stepIndex + 1 >= lesson.steps.size) {
            module.course.markCompleted(lesson.id, s.score)
            _state.value = s.copy(finished = true)
        } else {
            _state.value = enter(s, s.stepIndex + 1)
        }
    }

    fun speak(text: String) {
        scope.launch { module.tts.speak(text) }
    }

    fun restart() {
        val s = _state.value
        if (s.lesson == null) return
        _state.value = enter(s.copy(score = 0, finished = false), 0)
    }

    fun dispose() { scope.cancel() }
}
