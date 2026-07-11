package cx.viz.lancar.domain

import kotlin.math.roundToInt

data class BoxRow(val box: Int, val count: Int)

object ProgresStats {
    /** Overall answer accuracy as a 0-100 percent, or null when no answers recorded. */
    fun accuracyPct(correct: Int, wrong: Int): Int? {
        val total = correct + wrong
        if (total == 0) return null
        return (correct.toDouble() / total * 100).roundToInt()
    }

    /** Mastered fraction of the whole deck as a 0-100 percent (0 when deck is empty). */
    fun masteryPct(mastered: Int, total: Int): Int {
        if (total == 0) return 0
        return (mastered.toDouble() / total * 100).roundToInt()
    }

    /** Fixed [LeitnerScheduler.MAX_BOX]-row distribution; boxes absent from [boxCounts] render as 0. */
    fun boxRows(boxCounts: Map<Int, Int>): List<BoxRow> =
        (1..LeitnerScheduler.MAX_BOX).map { BoxRow(it, boxCounts[it] ?: 0) }
}
