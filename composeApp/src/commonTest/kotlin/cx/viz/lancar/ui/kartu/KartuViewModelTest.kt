package cx.viz.lancar.ui.kartu

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
import kotlin.test.assertTrue

private class FakeKartuContent : ContentRepository() {
    override suspend fun modules(): List<ModuleMeta> = listOf(
        ModuleMeta(MIXED_ID, "🎲 Mixed (all words)", 30),
        ModuleMeta("module-1", "Module 1", 10),
        ModuleMeta("module-2", "Module 2", 20),
    )
}

class KartuViewModelTest {
    private fun module(content: ContentRepository): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(content, ProgressRepository(db), SettingsRepository(db), NoopAudioPlayer())
    }

    @Test fun excludesMixedAndMapsRows() {
        val vm = KartuViewModel(module(FakeKartuContent()), dispatcher = Dispatchers.Unconfined)
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(listOf("module-1", "module-2"), s.modules.map { it.id })
        assertTrue(s.modules.none { it.id == MIXED_ID })
        assertEquals("Module 1", s.modules[0].title)
        assertEquals(10, s.modules[0].cardCount)
    }
}
