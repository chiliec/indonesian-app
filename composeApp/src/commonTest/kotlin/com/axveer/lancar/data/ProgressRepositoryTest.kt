package com.axveer.lancar.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.axveer.lancar.db.LancarDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressRepositoryTest {
    private fun repo(): ProgressRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        return ProgressRepository(LancarDatabase(driver))
    }

    @Test fun recordsCorrectAndWrong() {
        val r = repo()
        r.recordAnswer("a", correct = true)
        r.recordAnswer("a", correct = false)
        val p = r.forCards(listOf("a"))["a"]!!
        assertEquals(2, p.seen)
        assertEquals(1, p.correct)
        assertEquals(1, p.wrong)
    }

    @Test fun modulePercentReflectsMastery() {
        val r = repo()
        r.recordAnswer("a", correct = true)   // mastered
        r.recordAnswer("b", correct = false)  // not mastered
        assertEquals(50, r.modulePercent(listOf("a", "b")))
        assertEquals(0, r.modulePercent(emptyList()))
    }
}
