package cx.viz.lancar.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.db.LancarDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsRepositoryTest {
    private fun repo(): SettingsRepository {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        return SettingsRepository(LancarDatabase(driver))
    }

    @Test fun defaultsWhenEmpty() {
        val r = repo()
        assertNull(r.displayName())
        assertFalse(r.onboardingSeen())
        assertNull(r.accentName())
    }

    @Test fun roundTripsNameAccentAndFlag() {
        val r = repo()
        r.setDisplayName("Budi")
        r.setAccentName("GREEN")
        r.markOnboardingSeen()
        assertEquals("Budi", r.displayName())
        assertEquals("GREEN", r.accentName())
        assertTrue(r.onboardingSeen())
    }

    @Test fun blankNameReadsBackAsNull() {
        val r = repo()
        r.setDisplayName("   ")
        assertNull(r.displayName())
    }

    @Test fun listenTextDefaultsFalse() {
        assertFalse(repo().showListenText())
    }

    @Test fun listenTextRoundTrips() {
        val r = repo()
        r.setShowListenText(true)
        assertTrue(r.showListenText())
        r.setShowListenText(false)
        assertFalse(r.showListenText())
    }
}
