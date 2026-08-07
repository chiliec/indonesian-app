package cx.viz.lancar.ui.drill.quiz

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
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
class AudioPanelTest {

    @Test
    fun tileAndChipsReportTheRightRate() = runComposeUiTest {
        val plays = mutableListOf<Boolean>()
        setContent {
            AudioPanel(
                playing = false,
                word = "biasa",
                revealed = false,
                onPlay = { slow -> plays += slow },
                onToggleWord = {},
            )
        }
        onNodeWithText("Ketuk untuk memutar").assertExists()
        onNodeWithContentDescription("Putar audio · Play audio").performClick()
        onNodeWithText("Pelan · Slow").performClick()
        assertEquals(listOf(false, true), plays)
    }

    @Test
    fun playingSwapsTheHint() = runComposeUiTest {
        setContent {
            AudioPanel(playing = true, word = "biasa", revealed = false, onPlay = {}, onToggleWord = {})
        }
        onNodeWithText("Memutar…").assertExists()
    }

    @Test
    fun revealedShowsTheWordAndChipToggles() = runComposeUiTest {
        var toggles = 0
        setContent {
            AudioPanel(
                playing = false,
                word = "biasa",
                revealed = true,
                onPlay = {},
                onToggleWord = { toggles++ },
            )
        }
        onNodeWithText("biasa").assertExists()
        onNodeWithText("Lihat kata · Show word").performClick()
        assertEquals(1, toggles)
    }
}
