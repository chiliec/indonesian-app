package cx.viz.lancar.ui.drill.quiz

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], manifest = Config.NONE)
@OptIn(ExperimentalTestApi::class)
class FeedbackSheetTest {

    @Test
    fun correctSheetShowsAnswerAndAdvances() = runComposeUiTest {
        var next = 0
        setContent {
            FeedbackSheet(
                correct = true,
                word = "biasa",
                answer = "usual, ordinary, normal",
                note = null,
                isLast = false,
            ) { next++ }
        }
        onNodeWithText("Benar! · Correct").assertExists()
        onNodeWithText("“biasa” — usual, ordinary, normal").assertExists()
        onNodeWithText("Lanjut · Continue").performClick()
        assertEquals(1, next)
    }

    @Test
    fun wrongSheetShowsNoteAndFinalLabel() = runComposeUiTest {
        setContent {
            FeedbackSheet(
                correct = false,
                word = "sore",
                answer = "late afternoon",
                note = "dipakai sesudah jam 3",
                isLast = true,
            ) {}
        }
        onNodeWithText("Kurang tepat · Not quite").assertExists()
        onNodeWithText("dipakai sesudah jam 3").assertExists()
        onNodeWithText("Selesai ✓").assertExists()
    }
}
