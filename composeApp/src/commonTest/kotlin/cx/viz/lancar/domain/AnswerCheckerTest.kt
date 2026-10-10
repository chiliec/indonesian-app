package cx.viz.lancar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnswerCheckerTest {
    @Test fun normalizeLowercasesStripsPunctuationAndCollapsesSpaces() {
        assertEquals("saya tidak punya waktu", AnswerChecker.normalize("  Saya  tidak punya waktu! "))
        assertEquals("apa kabar", AnswerChecker.normalize("Apa kabar?"))
        assertEquals("laki laki", AnswerChecker.normalize("laki-laki"))
    }

    @Test fun checkTypeMatchesAnyAcceptedAnswerAfterNormalization() {
        val accept = listOf("saya tidak punya waktu", "saya tidak ada waktu")
        assertTrue(AnswerChecker.checkType("Saya tidak ada waktu.", accept))
        assertTrue(AnswerChecker.checkType("saya  tidak punya   waktu", accept))
        assertFalse(AnswerChecker.checkType("saya punya waktu", accept))
        assertFalse(AnswerChecker.checkType("", accept))
    }

    @Test fun checkBuildIsOrderSensitive() {
        val answer = listOf("rumah", "teman", "saya")
        assertTrue(AnswerChecker.checkBuild(listOf("rumah", "teman", "saya"), answer))
        assertFalse(AnswerChecker.checkBuild(listOf("saya", "teman", "rumah"), answer))
        assertFalse(AnswerChecker.checkBuild(listOf("rumah", "teman"), answer))
        assertFalse(AnswerChecker.checkBuild(emptyList(), answer))
    }
}
