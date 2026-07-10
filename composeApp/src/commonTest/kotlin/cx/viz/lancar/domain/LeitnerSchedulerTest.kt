package cx.viz.lancar.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LeitnerSchedulerTest {
    @Test fun firstMasterySeedsBoxOneDueTomorrow() {
        val s = LeitnerScheduler.onFirstMastery(today = 100L)
        assertEquals(1, s.box)
        assertEquals(101L, s.dueDay) // interval[1] = 1 day
    }

    @Test fun correctPromotesAndSchedulesByNewBoxInterval() {
        // box 1 -> 2, interval[2] = 3 days
        val s = LeitnerScheduler.onReview(Schedule(box = 1, dueDay = 100L), correct = true, today = 200L)
        assertEquals(2, s.box)
        assertEquals(203L, s.dueDay)
    }

    @Test fun correctAtMaxBoxStaysAtMaxWithLongestInterval() {
        // box 6 -> 6, interval[6] = 90 days
        val s = LeitnerScheduler.onReview(Schedule(box = 6, dueDay = 100L), correct = true, today = 500L)
        assertEquals(6, s.box)
        assertEquals(590L, s.dueDay)
    }

    @Test fun wrongResetsToBoxOneDueTomorrow() {
        val s = LeitnerScheduler.onReview(Schedule(box = 5, dueDay = 100L), correct = false, today = 300L)
        assertEquals(1, s.box)
        assertEquals(301L, s.dueDay)
    }

    @Test fun promotionIntervalsFollowTheLadder() {
        // box 2 -> 3 (interval 7), 3 -> 4 (16), 4 -> 5 (35)
        assertEquals(7L, LeitnerScheduler.onReview(Schedule(2, 0L), true, 0L).dueDay)
        assertEquals(16L, LeitnerScheduler.onReview(Schedule(3, 0L), true, 0L).dueDay)
        assertEquals(35L, LeitnerScheduler.onReview(Schedule(4, 0L), true, 0L).dueDay)
    }
}
