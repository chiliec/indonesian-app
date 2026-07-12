package cx.viz.lancar.ui.drill

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.Card
import cx.viz.lancar.platform.NoopAudioPlayer
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TEST_MODULE = "test-module"

private class FakeContent : ContentRepository() {
    private val cards = (1..12).map { i ->
        Card(id = "c-$i", indonesian = "kata-$i", english = "word-$i", audio = "c-$i.m4a")
    }
    override suspend fun cards(moduleId: String): List<Card> =
        if (moduleId == TEST_MODULE) cards else super.cards(moduleId)
}

class DrillViewModelTest {
    private fun module(): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(FakeContent(), ProgressRepository(db), SettingsRepository(db), NoopAudioPlayer())
    }

    @Test fun seedsRevealTextFromSettings() {
        val m = module()
        m.settings.setShowListenText(true)
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        assertTrue(vm.state.value.revealText)
    }

    @Test fun revealWordSetsFlag() {
        val m = module() // setting defaults false
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        assertFalse(vm.state.value.revealText)
        vm.revealWord()
        assertTrue(vm.state.value.revealText)
    }
}
