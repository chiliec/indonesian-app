package com.axveer.lancar.ui.onboarding

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.axveer.lancar.data.SettingsRepository
import com.axveer.lancar.db.LancarDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingViewModelTest {
    private fun settings(): SettingsRepository {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        return SettingsRepository(LancarDatabase(driver))
    }

    @Test fun stepsAdvanceAndClamp() {
        val vm = OnboardingViewModel(settings())
        assertEquals(0, vm.state.value.step)
        vm.next(); assertEquals(1, vm.state.value.step)
        vm.next(); assertEquals(1, vm.state.value.step) // clamped at LAST_STEP
        vm.back(); vm.back(); assertEquals(0, vm.state.value.step) // clamped at 0
    }

    @Test fun finishStoresNameAndMarksSeen() {
        val s = settings()
        val vm = OnboardingViewModel(s)
        vm.onNameChange("Budi")
        vm.finish()
        assertEquals("Budi", s.displayName())
        assertTrue(s.onboardingSeen())
    }

    @Test fun blankNameAllowed() {
        val s = settings()
        val vm = OnboardingViewModel(s)
        vm.finish()
        assertNull(s.displayName())
        assertTrue(s.onboardingSeen())
    }
}
