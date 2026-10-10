package cx.viz.lancar.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.db.LancarDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class CourseRepositoryTest {
    private fun repo(): CourseRepository {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        return CourseRepository(LancarDatabase(driver), now = { 1234L })
    }

    @Test fun emptyByDefault() {
        assertEquals(emptyMap(), repo().completed())
    }

    @Test fun markCompletedStoresScore() {
        val r = repo()
        r.markCompleted("l01", 7)
        assertEquals(mapOf("l01" to 7), r.completed())
    }

    @Test fun repeatKeepsBestScore() {
        val r = repo()
        r.markCompleted("l01", 7)
        r.markCompleted("l01", 3)
        assertEquals(7, r.completed()["l01"])
        r.markCompleted("l01", 9)
        assertEquals(9, r.completed()["l01"])
    }

    @Test fun resetClearsEverything() {
        val r = repo()
        r.markCompleted("l01", 7)
        r.markCompleted("l02", 2)
        r.reset()
        assertEquals(emptyMap(), r.completed())
    }
}
