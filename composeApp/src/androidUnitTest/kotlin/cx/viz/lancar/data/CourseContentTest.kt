package cx.viz.lancar.data

import cx.viz.lancar.domain.Step
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Validates the real bundled course.json. Gradle runs tests with cwd = composeApp/. */
class CourseContentTest {
    private val lessons by lazy {
        parseCourse(File("src/commonMain/composeResources/files/content/course.json").readText())
    }

    @Test fun lessonIdsAreUniqueAndNonEmpty() {
        assertTrue(lessons.isNotEmpty())
        assertEquals(lessons.size, lessons.map { it.id }.toSet().size, "duplicate lesson id")
        lessons.forEach { assertTrue(it.id.isNotBlank() && it.title.isNotBlank(), "blank id/title in ${it.id}") }
    }

    @Test fun everyLessonHasAtLeastOneExercise() {
        lessons.forEach { assertTrue(it.exerciseCount >= 1, "${it.id} has no exercises") }
    }

    @Test fun everyStepIsWellFormed() {
        lessons.forEach { lesson ->
            lesson.steps.forEachIndexed { i, step ->
                val where = "${lesson.id} step $i"
                when (step) {
                    is Step.Theory -> assertTrue(step.title.isNotBlank() && step.body.isNotBlank(), where)
                    is Step.Build -> {
                        assertTrue(step.answer.isNotEmpty(), "$where empty answer")
                        assertTrue(step.answer.all { it.isNotBlank() } && step.extra.all { it.isNotBlank() }, "$where blank tile")
                    }
                    is Step.Fill -> {
                        assertEquals(1, Regex("___").findAll(step.text).count(), "$where needs exactly one ___")
                        assertTrue(step.correct in step.options.indices, "$where correct out of range")
                        assertEquals(step.options.size, step.options.toSet().size, "$where duplicate options")
                        assertTrue(step.options.size >= 2, "$where needs 2+ options")
                    }
                    is Step.Choose -> {
                        assertTrue(step.correct in step.options.indices, "$where correct out of range")
                        assertEquals(step.options.size, step.options.toSet().size, "$where duplicate options")
                        assertTrue(step.options.size >= 2, "$where needs 2+ options")
                    }
                    is Step.Type -> {
                        assertTrue(step.accept.isNotEmpty() && step.accept.all { it.isNotBlank() }, "$where empty accept")
                    }
                }
            }
        }
    }
}
