package com.axveer.lancar.data

import com.axveer.lancar.db.LancarDatabase
import com.axveer.lancar.domain.CardProgress
import com.axveer.lancar.domain.MasteryCalculator

class ProgressRepository(private val db: LancarDatabase) {
    private val q = db.progressQueries

    fun recordAnswer(cardId: String, correct: Boolean) {
        // Two queries instead of a single UPSERT: insertOrIgnore seeds zeros, then updateAnswer
        // increments. Both must always run together; the intermediate zero-row state is private.
        q.insertOrIgnore(cardId = cardId)
        q.updateAnswer(
            cardId = cardId,
            correctInc = if (correct) 1L else 0L,
            wrongInc = if (correct) 0L else 1L,
            now = 0L,  // v1: last_seen unused; SRS scheduling is deferred
        )
    }

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
}
