package cx.viz.lancar.domain

/** SRS state for one card. [box] 0 = not scheduled; [dueDay] is an epoch-day. */
data class Schedule(val box: Int, val dueDay: Long)

/**
 * Leitner spaced repetition. Six boxes with day-based intervals. Pure: the caller
 * passes today's epoch-day in, so this stays testable and free of any clock.
 */
object LeitnerScheduler {
    const val MAX_BOX = 6

    // Indexed by box number; index 0 is an unused sentinel for "not in SRS".
    private val intervalDays = longArrayOf(0, 1, 3, 7, 16, 35, 90)

    fun onFirstMastery(today: Long): Schedule =
        Schedule(box = 1, dueDay = today + intervalDays[1])

    fun onReview(current: Schedule, correct: Boolean, today: Long): Schedule =
        if (correct) {
            val newBox = minOf(current.box + 1, MAX_BOX)
            Schedule(newBox, today + intervalDays[newBox])
        } else {
            Schedule(box = 1, dueDay = today + intervalDays[1]) // lapse: reset to box 1
        }
}
