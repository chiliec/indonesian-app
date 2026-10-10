package cx.viz.lancar.domain

object AnswerChecker {
    private val punctuation = Regex("[^\\p{L}\\p{N}\\s]")
    private val spaces = Regex("\\s+")

    /** Lowercase, punctuation → space, collapse whitespace, trim. */
    fun normalize(s: String): String =
        s.lowercase().replace(punctuation, " ").replace(spaces, " ").trim()

    fun checkBuild(tiles: List<String>, answer: List<String>): Boolean =
        tiles.map(::normalize) == answer.map(::normalize)

    fun checkType(input: String, accept: List<String>): Boolean {
        val n = normalize(input)
        return n.isNotEmpty() && accept.any { normalize(it) == n }
    }
}
