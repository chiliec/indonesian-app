package cx.viz.lancar.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.db.LancarDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressRepositoryTest {
    private fun repo(today: () -> Long = { 0L }): ProgressRepository {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        return ProgressRepository(LancarDatabase(driver), today)
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

    @Test fun resetClearsMastery() {
        val r = repo()
        r.recordAnswer("a", correct = true)
        r.reset()
        assertEquals(0, r.modulePercent(listOf("a")))
        assertEquals(emptyMap(), r.forCards(listOf("a")))
    }

    @Test fun firstCorrectSeedsDueOneDayOut() {
        val r = repo { 10L }
        r.recordAnswer("a", correct = true)        // masters "a" on day 10 -> due day 11
        assertEquals(emptyList(), r.dueCardIds(10)) // not due on day 10
    }

    @Test fun dueCardsSurfaceOnceTheirDayArrives() {
        var day = 10L
        val r = repo { day }
        r.recordAnswer("a", correct = true) // due day 11
        r.recordAnswer("b", correct = true) // due day 11
        day = 100L
        assertEquals(listOf("a", "b"), r.dueCardIds(10).sorted())
        assertEquals(2, r.countDue())
    }

    @Test fun dueCardsOrderMostOverdueFirst() {
        var day = 10L
        val r = repo { day }
        r.recordAnswer("early", correct = true) // due day 11
        day = 20L
        r.recordAnswer("late", correct = true)  // due day 21
        day = 100L
        assertEquals(listOf("early", "late"), r.dueCardIds(10))
    }

    @Test fun dueQueryRespectsLimit() {
        var day = 10L
        val r = repo { day }
        r.recordAnswer("a", correct = true)
        r.recordAnswer("b", correct = true)
        r.recordAnswer("c", correct = true)
        day = 100L
        assertEquals(2, r.dueCardIds(2).size)
    }

    @Test fun reviewCorrectPushesDueDateFurtherOut() {
        var day = 10L
        val r = repo { day }
        r.recordAnswer("a", correct = true) // box 1, due day 11
        day = 11L
        r.recordReview("a", correct = true) // box 2, due day 11 + 3 = 14
        day = 13L
        assertEquals(emptyList(), r.dueCardIds(10)) // not due yet on day 13
        day = 14L
        assertEquals(listOf("a"), r.dueCardIds(10))
    }

    @Test fun reviewWrongBringsCardBackNextDay() {
        var day = 10L
        val r = repo { day }
        r.recordAnswer("a", correct = true) // box 1, due day 11
        day = 30L
        r.recordReview("a", correct = false) // lapse -> due day 31
        day = 31L
        assertEquals(listOf("a"), r.dueCardIds(10))
        // mastery stays sticky despite the wrong review
        assertEquals(100, r.modulePercent(listOf("a")))
    }

    @Test fun aggregatesSummariseProgress() {
        val r = repo { 10L }
        r.recordAnswer("a", correct = true)   // mastered, box 1
        r.recordAnswer("a", correct = false)  // still mastered, +1 wrong
        r.recordAnswer("b", correct = true)   // mastered, box 1
        r.recordAnswer("c", correct = false)  // seen, not mastered, box 0

        assertEquals(2, r.masteredCount())     // a, b
        assertEquals(3, r.seenCount())         // a, b, c
        val t = r.totals()
        assertEquals(2, t.correct)             // a:1 + b:1
        assertEquals(2, t.wrong)               // a:1 + c:1
        assertEquals(2, r.reviewDeckCount())   // a, b in box > 0
        assertEquals(mapOf(1 to 2), r.boxCounts())
    }

    @Test fun aggregatesEmptyOnFreshDb() {
        val r = repo()
        assertEquals(0, r.masteredCount())
        assertEquals(0, r.seenCount())
        assertEquals(0, r.totals().correct)
        assertEquals(0, r.totals().wrong)
        assertEquals(0, r.reviewDeckCount())
        assertEquals(emptyMap(), r.boxCounts())
    }

    @Test fun countDueWithinCountsHorizon() {
        var day = 10L
        val r = repo { day }
        r.recordAnswer("a", correct = true) // day 10 -> due day 11
        day = 11L
        r.recordReview("a", correct = true) // box 2 -> due day 11 + 3 = 14
        r.recordAnswer("b", correct = true) // day 11 -> due day 12
        assertEquals(0, r.countDue())         // nothing due on day 11
        assertEquals(1, r.countDueWithin(1))  // due_day <= 12 -> b
        assertEquals(2, r.countDueWithin(3))  // due_day <= 14 -> a, b
    }
}
