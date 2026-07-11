package cx.viz.lancar.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PronunciationMatcherTest {
    @Test fun exactMatch() = assertTrue(PronunciationMatcher.matches("makan", "makan"))

    @Test fun caseAndPunctuationIgnored() {
        assertTrue(PronunciationMatcher.matches("Makan.", "makan"))
        assertTrue(PronunciationMatcher.matches("apa kabar", "apa kabar?"))
    }

    @Test fun surroundingAndInnerWhitespaceNormalized() {
        assertTrue(PronunciationMatcher.matches("  terima   kasih ", "terima kasih"))
    }

    @Test fun oneEditToleratedForLongerWords() {
        // recognizer near-miss on a >4-char target
        assertTrue(PronunciationMatcher.matches("terimah kasih", "terima kasih"))
    }

    @Test fun shortWordsRequireExactMatch() {
        // "satu" is 4 chars -> zero tolerance
        assertFalse(PronunciationMatcher.matches("sate", "satu"))
    }

    @Test fun clearMismatchRejected() {
        assertFalse(PronunciationMatcher.matches("kabar", "makan"))
    }

    @Test fun emptyTranscriptRejected() {
        assertFalse(PronunciationMatcher.matches("", "makan"))
        assertFalse(PronunciationMatcher.matches("   ", "makan"))
    }
}
