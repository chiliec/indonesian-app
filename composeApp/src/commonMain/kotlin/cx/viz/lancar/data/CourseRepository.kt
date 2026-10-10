package cx.viz.lancar.data

import cx.viz.lancar.db.LancarDatabase
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Lesson completion for the Kursus tab. Best score per lesson, never lowered. */
@OptIn(ExperimentalTime::class)
class CourseRepository(
    db: LancarDatabase,
    private val now: () -> Long = { Clock.System.now().epochSeconds },
) {
    private val q = db.courseQueries

    fun completed(): Map<String, Int> =
        q.selectCompleted().executeAsList().associate { it.lesson_id to it.best_score.toInt() }

    fun markCompleted(lessonId: String, score: Int) = q.transaction {
        q.insertOrIgnore(lessonId = lessonId, now = now(), score = score.toLong())
        q.updateBest(now = now(), score = score.toLong(), lessonId = lessonId)
    }

    fun reset() = q.deleteAll()
}
