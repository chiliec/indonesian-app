package cx.viz.lancar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MasteryCalculatorTest {
    @Test fun masteredWhenCorrectPositive() {
        assertTrue(MasteryCalculator.isMastered(CardProgress("a", correct = 1)))
        assertFalse(MasteryCalculator.isMastered(CardProgress("a", correct = 0, wrong = 3)))
        assertFalse(MasteryCalculator.isMastered(null))
    }

    @Test fun emptyModuleIsZeroPercent() {
        assertEquals(0, MasteryCalculator.modulePercent(emptyList(), emptyMap()))
    }

    @Test fun halfMasteredRoundsCorrectly() {
        val prog = mapOf(
            "a" to CardProgress("a", correct = 1),
            "b" to CardProgress("b", correct = 2),
        )
        // 2 of 4 mastered -> 50
        assertEquals(50, MasteryCalculator.modulePercent(listOf("a", "b", "c", "d"), prog))
    }
}
