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
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeKursusContent : ContentRepository() {
    override suspend fun course(): List<Lesson> = listOf("a", "b", "c").map { id ->
        Lesson(id, id.uppercase(), "sub $id", listOf(
            Step.Theory("t", "b"),
            Step.Choose("p", listOf("x", "y"), 0),
            Step.Choose("q", listOf("x", "y"), 1),
        ))
    }
}

class KursusViewModelTest {
    private fun module(): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(FakeKursusContent(), ProgressRepository(db), SettingsRepository(db), NoopAudioPlayer())
    }

    private fun states(m: AppModule) =
        KursusViewModel(m, dispatcher = Dispatchers.Unconfined).state.value.rows.map { it.state }

    @Test fun nothingCompletedOnlyFirstIsCurrent() {
        val m = module()
        assertEquals(listOf(RowState.Current, RowState.Locked, RowState.Locked), states(m))
        val s = KursusViewModel(m, dispatcher = Dispatchers.Unconfined).state.value
        assertEquals(0, s.completedCount)
        assertEquals(3, s.totalCount)
        assertEquals(listOf(1, 2, 3), s.rows.map { it.index })
    }

    @Test fun someCompletedShowsDoneCurrentLocked() {
        val m = module()
        m.course.markCompleted("a", 2)
        assertEquals(listOf(RowState.Done(2, 2), RowState.Current, RowState.Locked), states(m))
        assertEquals(1, KursusViewModel(m, dispatcher = Dispatchers.Unconfined).state.value.completedCount)
    }

    @Test fun allCompletedAllDone() {
        val m = module()
        listOf("a", "b", "c").forEach { m.course.markCompleted(it, 1) }
        assertEquals(List(3) { RowState.Done(1, 2) }, states(m))
    }
}
