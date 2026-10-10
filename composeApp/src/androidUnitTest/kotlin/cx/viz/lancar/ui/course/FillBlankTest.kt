package cx.viz.lancar.ui.course

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
class FillBlankTest {

    @Test
    fun selectingAnOptionFillsTheGap() = runComposeUiTest {
        var selected by mutableStateOf<Int?>(null)
        setContent {
            FillBlank(
                text = "Kamu tinggal ___ mana?",
                options = listOf("di", "ke", "dari"),
                selected = selected,
                checked = false,
                correctIndex = 0,
                onSelect = { selected = it },
            )
        }
        onNodeWithText("Kamu tinggal _____ mana?").assertExists()
        onNodeWithText("ke").performClick()
        assertEquals(1, selected)
        onNodeWithText("Kamu tinggal ke mana?").assertExists()
        onNodeWithText("di").performClick()
        assertEquals(0, selected)
        onNodeWithText("Kamu tinggal di mana?").assertExists()
    }

    @Test
    fun checkedStateDisablesOptions() = runComposeUiTest {
        var selected by mutableStateOf<Int?>(1)
        setContent {
            FillBlank("Kamu tinggal ___ mana?", listOf("di", "ke", "dari"), selected, checked = true, correctIndex = 0) { selected = it }
        }
        onNodeWithText("dari").performClick()
        assertEquals(1, selected)
        onNodeWithText("✓").assertExists()   // correct row shows tick
        onNodeWithText("✕").assertExists()   // wrong selected row shows cross
    }
}
