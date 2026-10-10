package cx.viz.lancar.ui.course

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
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
class BuildSentenceTest {
    private val tiles = listOf("saya", "rumah", "teman", "besar")

    @Test
    fun tappingPoolTilePlacesItAndTappingPlacedTileReturnsIt() = runComposeUiTest {
        var placed by mutableStateOf(listOf<Int>())
        setContent {
            BuildSentence(
                prompt = "my friend's house",
                tiles = tiles,
                placed = placed,
                enabled = true,
                onPlace = { placed = placed + it },
                onRemove = { placed = placed - it },
            )
        }
        onNodeWithText("rumah").performClick()
        assertEquals(listOf(1), placed)
        onNodeWithText("teman").performClick()
        assertEquals(listOf(1, 2), placed)
        // a placed tile is drawn once (in the slots), never twice
        onAllNodesWithText("rumah").assertCountEquals(1)
        onNodeWithText("rumah").performClick()   // tap placed tile → back to pool
        assertEquals(listOf(2), placed)
    }

    @Test
    fun disabledIgnoresTaps() = runComposeUiTest {
        var placed by mutableStateOf(listOf(0))
        setContent {
            BuildSentence("x", tiles, placed, enabled = false, onPlace = { placed = placed + it }, onRemove = { placed = placed - it })
        }
        onNodeWithText("rumah").performClick()
        onNodeWithText("saya").performClick()
        assertEquals(listOf(0), placed)
    }
}
