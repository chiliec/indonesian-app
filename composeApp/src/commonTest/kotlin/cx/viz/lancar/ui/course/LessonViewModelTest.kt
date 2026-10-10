package cx.viz.lancar.ui.course

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.Lesson
import cx.viz.lancar.domain.Step
import cx.viz.lancar.platform.NoopAudioPlayer
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeCourseContent : ContentRepository() {
    override suspend fun course(): List<Lesson> = listOf(
        Lesson("a", "A", "", listOf(
            Step.Theory("T", "body"),
            Step.Choose("pick", listOf("right", "wrong1", "wrong2"), correct = 0),
            Step.Type("type ya", accept = listOf("ya")),
        )),
        Lesson("b", "B", "", listOf(Step.Choose("pick", listOf("x", "y"), correct = 1))),
    )
}

class LessonViewModelTest {
    private fun module(): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(FakeCourseContent(), ProgressRepository(db), SettingsRepository(db), NoopAudioPlayer())
    }

    private fun vm(m: AppModule = module(), id: String = "a") =
        LessonViewModel(m, id, random = Random(1), dispatcher = Dispatchers.Unconfined)

    @Test fun loadsLessonAndNextId() {
        val v = vm()
        val s = v.state.value
        assertFalse(s.loading)
        assertEquals("a", s.lesson?.id)
        assertEquals(2, s.exerciseCount)
        assertEquals("b", s.nextLessonId)
    }

    @Test fun unknownIdGivesEmptyState() {
        val s = vm(id = "zzz").state.value
        assertFalse(s.loading)
        assertNull(s.lesson)
    }

    @Test fun theoryAdvancesWithoutCheck() {
        val v = vm()
        assertFalse(v.state.value.canCheck)
        v.next()
        assertEquals(1, v.state.value.stepIndex)
        assertTrue(v.state.value.step is Step.Choose)
        assertEquals(3, v.state.value.options.size)
        assertEquals("right", v.state.value.options[v.state.value.correctIndex])
    }

    @Test fun wrongAnswerDoesNotScoreAndShowsExpected() {
        val v = vm()
        v.next()
        val wrong = (v.state.value.correctIndex + 1) % 3
        v.selectOption(wrong)
        assertTrue(v.state.value.canCheck)
        v.check()
        val s = v.state.value
        assertTrue(s.checked)
        assertEquals(false, s.correct)
        assertEquals(0, s.score)
        assertEquals("right", s.expectedAnswer)
        assertFalse(s.canCheck)
    }

    @Test fun nextWithoutCheckIsNoOpOnExercise() {
        val v = vm()
        v.next()
        v.next()
        assertEquals(1, v.state.value.stepIndex)
    }

    @Test fun lastNextMarksCompletedWithScore() {
        val m = module()
        val v = vm(m)
        v.next()                                    // theory → choose
        v.selectOption(v.state.value.correctIndex)
        v.check()                                   // score 1
        v.next()                                    // → type
        v.setTyped("Ya!")
        v.check()                                   // score 2
        v.next()                                    // finish
        assertTrue(v.state.value.finished)
        assertEquals(2, v.state.value.score)
        assertEquals(mapOf("a" to 2), m.course.completed())
    }

    @Test fun restartResetsScoreAndStep() {
        val v = vm()
        v.next()
        v.selectOption(v.state.value.correctIndex)
        v.check()
        v.restart()
        val s = v.state.value
        assertEquals(0, s.stepIndex)
        assertEquals(0, s.score)
        assertFalse(s.checked)
        assertFalse(s.finished)
    }
}
