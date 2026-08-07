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
class OptionRowTest {

    @Test
    fun idleRowShowsKeyAndClicks() = runComposeUiTest {
        var clicks = 0
        setContent {
            OptionRow("hospital", key = "B", state = OptState.IDLE, enabled = true) { clicks++ }
        }
        onNodeWithText("B").assertExists()
        onNodeWithText("hospital").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun correctRowShowsTickInsteadOfKey() = runComposeUiTest {
        setContent {
            OptionRow("hospital", key = "B", state = OptState.CORRECT, enabled = false) {}
        }
        onNodeWithText("✓").assertExists()
        onNodeWithText("B").assertDoesNotExist()
    }

    @Test
    fun wrongRowShowsCross() = runComposeUiTest {
        setContent {
            OptionRow("market", key = "C", state = OptState.WRONG, enabled = false) {}
        }
        onNodeWithText("✕").assertExists()
    }

    @Test
    fun disabledRowDoesNotClick() = runComposeUiTest {
        var clicks = 0
        setContent {
            OptionRow("market", key = "C", state = OptState.DIMMED, enabled = false) { clicks++ }
        }
        onNodeWithText("market").performClick()
        assertEquals(0, clicks)
    }
}
