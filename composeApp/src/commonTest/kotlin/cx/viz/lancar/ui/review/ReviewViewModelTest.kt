package cx.viz.lancar.ui.review

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.MIXED_ID
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.Card
import cx.viz.lancar.domain.ModuleMeta
import cx.viz.lancar.platform.NoopAudioPlayer
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeReviewContent : ContentRepository() {
    private val m1 = listOf(
        Card(id = "a", indonesian = "satu", english = "one"),
        Card(id = "b", indonesian = "dua", english = "two"),
    )
    private val m2 = listOf(
        Card(id = "c", indonesian = "makan", english = "eat"),
        Card(id = "d", indonesian = "minum", english = "drink"),
    )
    override suspend fun modules(): List<ModuleMeta> = listOf(
        ModuleMeta(MIXED_ID, "Mixed", 4),
        ModuleMeta("m1", "M1", 2),
        ModuleMeta("m2", "M2", 2),
    )
    override suspend fun cards(moduleId: String): List<Card> = when (moduleId) {
        "m1" -> m1
        "m2" -> m2
        else -> m1 + m2 // MIXED
    }
}

class ReviewViewModelTest {
    private fun setup(today: () -> Long): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(FakeReviewContent(), ProgressRepository(db, today), SettingsRepository(db), NoopAudioPlayer())
    }

    @Test fun queueContainsOnlyDueCardsAcrossModules() {
        var day = 10L
        val module = setup { day }
        // Master one card from each module so both are scheduled.
        module.progress.recordAnswer("a", correct = true) // m1, due 11
        module.progress.recordAnswer("c", correct = true) // m2, due 11
        day = 100L
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        val s = vm.state.value
        assertTrue(s.question != null)
        assertEquals(2, s.total)
        // The current question's card is one of the two due cards.
        assertTrue(s.question!!.card.id in listOf("a", "c"))
    }

    @Test fun emptyWhenNothingDue() {
        val module = setup { 100L } // nothing mastered -> nothing scheduled
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        assertTrue(vm.state.value.finished)
        assertEquals(0, vm.state.value.total)
    }
}
