package cx.viz.lancar.domain

object PronunciationMatcher {

    fun matches(transcript: String, target: String): Boolean {
        val a = normalize(transcript)
        val b = normalize(target)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        val tolerance = if (b.length <= 4) 0 else 1
        return levenshtein(a, b) <= tolerance
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ")

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,
                    curr[j - 1] + 1,
                    prev[j - 1] + cost,
                )
            }
            prev.indices.forEach { prev[it] = curr[it] }
        }
        return prev[b.length]
    }
}
