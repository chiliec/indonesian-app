package com.axveer.lancar.ui.profile

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.axveer.lancar.data.ContentRepository
import com.axveer.lancar.data.ProgressRepository
import com.axveer.lancar.data.SettingsRepository
import com.axveer.lancar.db.LancarDatabase
import com.axveer.lancar.platform.NoopAudioPlayer
import com.axveer.lancar.ui.AppModule
import com.axveer.lancar.ui.theme.Accent
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileViewModelTest {
    private fun module(): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(
            content = ContentRepository(),
            progress = ProgressRepository(db),
            settings = SettingsRepository(db),
            audio = NoopAudioPlayer(),
        )
    }

    @Test fun setAccentPersistsAndUpdatesState() {
        val m = module()
        val vm = ProfileViewModel(m)
        vm.setAccent(Accent.BLUE)
        assertEquals(Accent.BLUE, vm.state.value.accent)
        assertEquals("BLUE", m.settings.accentName())
        assertEquals(Accent.BLUE, m.accent.value)
    }

    @Test fun setNamePersistsAndUpdatesState() {
        val m = module()
        val vm = ProfileViewModel(m)
        vm.setName("Sari")
        assertEquals("Sari", vm.state.value.name)
        assertEquals("Sari", m.settings.displayName())
    }

    @Test fun resetProgressClearsMastery() {
        val m = module()
        m.progress.recordAnswer("a", correct = true)
        ProfileViewModel(m).resetProgress()
        assertEquals(0, m.progress.modulePercent(listOf("a")))
    }
}
