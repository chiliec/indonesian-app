package cx.viz.lancar.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lesson(val id: String, val title: String, val subtitle: String, val steps: List<Step>) {
    /** Number of graded steps (everything except theory). */
    val exerciseCount: Int get() = steps.count { it !is Step.Theory }
}

@Serializable
sealed class Step {
    @Serializable @SerialName("theory")
    data class Theory(val title: String, val body: String, val examples: List<Example> = emptyList()) : Step()

    @Serializable @SerialName("build")
    data class Build(val prompt: String, val answer: List<String>, val extra: List<String> = emptyList(), val explain: String? = null) : Step()

    @Serializable @SerialName("fill")
    data class Fill(val text: String, val options: List<String>, val correct: Int, val explain: String? = null) : Step()

    @Serializable @SerialName("choose")
    data class Choose(val prompt: String, val options: List<String>, val correct: Int, val explain: String? = null) : Step()

    @Serializable @SerialName("type")
    data class Type(val prompt: String, val accept: List<String>, val explain: String? = null) : Step()
}

@Serializable
data class Example(val id: String, val en: String)
