package cx.viz.lancar.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class CourseUnlockTest {
    private val order = listOf("l01", "l02", "l03", "l04")

    @Test fun nothingCompletedUnlocksOnlyFirst() {
        assertEquals(setOf("l01"), unlockedLessonIds(order, emptySet()))
    }

    @Test fun completedPlusNextIncomplete() {
        assertEquals(setOf("l01", "l02", "l03"), unlockedLessonIds(order, setOf("l01", "l02")))
    }

    @Test fun gapOnlyOpensFirstIncomplete() {
        // l02 was never done: l04 stays locked even though l03 is done
        assertEquals(setOf("l01", "l02", "l03"), unlockedLessonIds(order, setOf("l01", "l03")))
    }

    @Test fun allCompletedUnlocksAll() {
        assertEquals(order.toSet(), unlockedLessonIds(order, order.toSet()))
    }

    @Test fun emptyOrderIsEmpty() {
        assertEquals(emptySet(), unlockedLessonIds(emptyList(), emptySet()))
    }
}
