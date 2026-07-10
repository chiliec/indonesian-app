package cx.viz.lancar.data

import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.CardProgress
import cx.viz.lancar.domain.LeitnerScheduler
import cx.viz.lancar.domain.MasteryCalculator
import cx.viz.lancar.domain.Schedule
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class ProgressRepository(
    private val db: LancarDatabase,
    private val today: () -> Long = {
        Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()
    },
) {
    private val q = db.progressQueries

    /** Record a drill answer. Seeds SRS the first time the card becomes mastered. */
    fun recordAnswer(cardId: String, correct: Boolean) {
        val wasMastered = isMastered(cardId)
        q.insertOrIgnore(cardId = cardId)
        q.updateAnswer(
            cardId = cardId,
            correctInc = if (correct) 1L else 0L,
            wrongInc = if (correct) 0L else 1L,
            now = today(),
        )
        if (correct && !wasMastered) {
            val s = LeitnerScheduler.onFirstMastery(today())
            q.updateSchedule(box = s.box.toLong(), dueDay = s.dueDay, lastSeen = today(), cardId = cardId)
        }
    }

    /** Record a review answer and re-schedule the card via Leitner. */
    fun recordReview(cardId: String, correct: Boolean) {
        q.insertOrIgnore(cardId = cardId)
        q.updateAnswer(
            cardId = cardId,
            correctInc = if (correct) 1L else 0L,
            wrongInc = if (correct) 0L else 1L,
            now = today(),
        )
        val current = scheduleOf(cardId) ?: Schedule(box = 1, dueDay = today())
        val next = LeitnerScheduler.onReview(current, correct, today())
        q.updateSchedule(box = next.box.toLong(), dueDay = next.dueDay, lastSeen = today(), cardId = cardId)
    }

    fun dueCardIds(limit: Int): List<String> =
        q.selectDue(today = today(), limit = limit.toLong()).executeAsList()

    fun countDue(): Int = q.countDue(today = today()).executeAsOne().toInt()

    fun forCards(ids: List<String>): Map<String, CardProgress> {
        if (ids.isEmpty()) return emptyMap()
        return q.selectByIds(ids).executeAsList().associate { row ->
            row.card_id to CardProgress(
                cardId = row.card_id,
                seen = row.seen.toInt(),
                correct = row.correct.toInt(),
                wrong = row.wrong.toInt(),
            )
        }
    }

    fun modulePercent(cardIds: List<String>): Int =
        MasteryCalculator.modulePercent(cardIds, forCards(cardIds))

    fun reset() = q.deleteAll()

    private fun isMastered(cardId: String): Boolean =
        MasteryCalculator.isMastered(forCards(listOf(cardId))[cardId])

    private fun scheduleOf(cardId: String): Schedule? =
        q.selectSchedule(cardId).executeAsOneOrNull()?.let { row ->
            val due = row.due_day ?: return null
            Schedule(box = row.box.toInt(), dueDay = due)
        }
}
