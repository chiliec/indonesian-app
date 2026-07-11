package cx.viz.lancar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgresStatsTest {
    @Test fun accuracyIsPercentOfAllAnswers() {
        assertEquals(50, ProgresStats.accuracyPct(correct = 2, wrong = 2))
        assertEquals(75, ProgresStats.accuracyPct(correct = 3, wrong = 1))
    }

    @Test fun accuracyIsNullWhenNoAnswers() {
        assertNull(ProgresStats.accuracyPct(correct = 0, wrong = 0))
    }

    @Test fun masteryIsPercentOfDeckAndZeroWhenEmpty() {
        assertEquals(40, ProgresStats.masteryPct(mastered = 2, total = 5))
        assertEquals(0, ProgresStats.masteryPct(mastered = 0, total = 0))
    }

    @Test fun boxRowsAlwaysHaveSixEntriesMissingBoxesZero() {
        val rows = ProgresStats.boxRows(mapOf(1 to 7, 3 to 2))
        assertEquals(6, rows.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), rows.map { it.box })
        assertEquals(7, rows[0].count)
        assertEquals(0, rows[1].count)
        assertEquals(2, rows[2].count)
        assertEquals(0, rows[5].count)
    }
}
