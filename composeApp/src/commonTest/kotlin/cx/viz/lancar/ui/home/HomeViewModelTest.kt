package cx.viz.lancar.ui.home

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

private class FakeHomeContent : ContentRepository() {
    override suspend fun modules(): List<ModuleMeta> =
        listOf(ModuleMeta(MIXED_ID, "Mixed", 2), ModuleMeta("m1", "M1", 2))
    override suspend fun cards(moduleId: String): List<Card> = listOf(
        Card(id = "a", indonesian = "satu", english = "one"),
        Card(id = "b", indonesian = "dua", english = "two"),
    )
}

class HomeViewModelTest {
    private fun setup(today: () -> Long): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(FakeHomeContent(), ProgressRepository(db, today), SettingsRepository(db), NoopAudioPlayer())
    }

    @Test fun dueCountReflectsScheduledCards() {
        var day = 10L
        val module = setup { day }
        module.progress.recordAnswer("a", correct = true) // due day 11
        day = 100L
        val vm = HomeViewModel(module, dispatcher = Dispatchers.Unconfined)
        assertEquals(1, vm.state.value.dueCount)
    }

    @Test fun dueCountZeroWhenNothingScheduled() {
        val vm = HomeViewModel(setup { 100L }, dispatcher = Dispatchers.Unconfined)
        assertEquals(0, vm.state.value.dueCount)
    }
}
