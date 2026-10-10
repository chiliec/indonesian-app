package cx.viz.lancar.domain

object AnswerChecker {
    /**
     * Lowercase, punctuation → space, collapse whitespace, trim.
     *
     * Uses Char.isLetterOrDigit() instead of a `\p{L}\p{N}` regex: Kotlin/Native's regex engine
     * doesn't support Unicode property escapes (throws PatternSyntaxException on iOS), while
     * Char.isLetterOrDigit() is plain stdlib and behaves identically on every target.
     */
    fun normalize(s: String): String {
        val sb = StringBuilder()
        var lastSpace = false
        for (c in s.lowercase()) {
            if (c.isLetterOrDigit()) {
                sb.append(c)
                lastSpace = false
            } else if (!lastSpace) {
                sb.append(' ')
                lastSpace = true
            }
        }
        return sb.toString().trim()
    }

    fun checkBuild(tiles: List<String>, answer: List<String>): Boolean =
        tiles.map(::normalize) == answer.map(::normalize)

    fun checkType(input: String, accept: List<String>): Boolean {
        val n = normalize(input)
        return n.isNotEmpty() && accept.any { normalize(it) == n }
    }
}
