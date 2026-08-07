package cx.viz.lancar.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.Card
import cx.viz.lancar.platform.NoopAudioPlayer
import cx.viz.lancar.ui.drill.DrillScreen
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.Test

private const val CHECK_MODULE = "check-module"

private class CheckContentRepository : ContentRepository() {
    private val fakes = (1..12).map { i ->
        Card(id = "card-$i", indonesian = "kata-$i", english = "word-$i", note = "note-$i")
    }
    override suspend fun cards(moduleId: String): List<Card> =
        if (moduleId == CHECK_MODULE) fakes else super.cards(moduleId)
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], manifest = Config.NONE)
@OptIn(ExperimentalTestApi::class)
class QuizCheckFlowTest {

    @Test
    fun answerCommitsOnlyAfterCheck() = runComposeUiTest {
        Class.forName("org.sqlite.JDBC")
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        val settings = SettingsRepository(db)
        settings.setAutoPlayAudio(false) // keep the equalizer idle in tests
        val module = AppModule(
            content = CheckContentRepository(),
            progress = ProgressRepository(db),
            settings = settings,
            audio = NoopAudioPlayer(),
        )
        setContent {
            DrillScreen(
                appModule = module,
                moduleId = CHECK_MODULE,
                onFinish = { _, _, _ -> },
                onBack = {},
            )
        }
        mainClock.advanceTimeBy(5_000)

        // Nothing committed yet: no feedback sheet, Check button present.
        onNodeWithText("Periksa · Check").assertExists()
        onNodeWithText("Lanjut · Continue").assertDoesNotExist()

        // Select the first option — still no sheet.
        onNodeWithText("word-1").performClick()
        mainClock.advanceTimeBy(1_000)
        onNodeWithText("Lanjut · Continue").assertDoesNotExist()

        // Check commits and the sheet slides up.
        // Robolectric's default test window is only 320x470dp; the redesigned quiz screen
        // (audio panel + 4 options + Check button, scrollable) overflows it, so the Check
        // button starts outside the viewport. performScrollTo() brings it into view first —
        // without it, performClick() taps the node's collapsed (0,0,0,0) off-screen bounds
        // and silently no-ops. See task-8-report.md for the diagnosis.
        onNodeWithText("Periksa · Check").performScrollTo().performClick()
        mainClock.advanceTimeBy(1_000)
        onNodeWithText("Lanjut · Continue").assertExists()

        // Advancing to the next question drops the sheet immediately and resets to the
        // pre-answer state — regression guard for the fadeOut answer-leak bug.
        onNodeWithText("Lanjut · Continue").performClick()
        mainClock.advanceTimeBy(1_000)
        onNodeWithText("Periksa · Check").assertExists()
    }
}
