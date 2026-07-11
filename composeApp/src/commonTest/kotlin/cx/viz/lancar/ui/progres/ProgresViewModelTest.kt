package cx.viz.lancar.ui.progres

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.MIXED_ID
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.ModuleMeta
import cx.viz.lancar.platform.NoopAudioPlayer
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

private class FakeProgresContent : ContentRepository() {
    override suspend fun modules(): List<ModuleMeta> = listOf(
        ModuleMeta(MIXED_ID, "🎲 Mixed (all words)", 100),
        ModuleMeta("m1", "M1", 3),
        ModuleMeta("m2", "M2", 2),
    )
}

class ProgresViewModelTest {
    private fun setup(content: ContentRepository, today: () -> Long): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(content, ProgressRepository(db, today), SettingsRepository(db), NoopAudioPlayer())
    }

    @Test fun assemblesSummaryAndBoxDistribution() {
        var day = 10L
        val module = setup(FakeProgresContent()) { day }
        module.progress.recordAnswer("a", correct = true)  // mastered, box 1, due 11
        module.progress.recordAnswer("a", correct = false) // still mastered, +1 wrong
        module.progress.recordAnswer("b", correct = true)  // mastered, box 1, due 11
        module.progress.recordAnswer("c", correct = false) // seen, not mastered
        day = 100L

        val vm = ProgresViewModel(module, dispatcher = Dispatchers.Unconfined)
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(2, s.mastered)
        assertEquals(5, s.total)          // 3 + 2; MIXED excluded
        assertEquals(40, s.masteryPct)    // 2/5
        assertEquals(50, s.accuracyPct)   // correct 2 / (2 + 2)
        assertEquals(3, s.seen)
        assertEquals(2, s.reviewDeck)
        assertEquals(6, s.boxRows.size)
        assertEquals(2, s.boxRows[0].count) // box 1 = a, b
        assertEquals(0, s.boxRows[5].count) // box 6 empty
        assertEquals(2, s.dueToday)         // a, b due day 11, today 100
    }

    @Test fun emptyStateWhenNothingMastered() {
        val vm = ProgresViewModel(setup(FakeProgresContent()) { 0L }, dispatcher = Dispatchers.Unconfined)
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(0, s.mastered)
        assertEquals(5, s.total)
        assertEquals(0, s.masteryPct)
        assertNull(s.accuracyPct)
        assertEquals(0, s.reviewDeck)
    }
}
