package com.axveer.lancar.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.axveer.lancar.data.ContentRepository
import com.axveer.lancar.data.ProgressRepository
import com.axveer.lancar.db.LancarDatabase
import com.axveer.lancar.domain.Card
import com.axveer.lancar.platform.NoopAudioPlayer
import com.axveer.lancar.ui.drill.DrillScreen
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.Test

private const val TEST_MODULE = "test-module"

/** Generates 12 fake cards so SESSION_SIZE is satisfied without bundled resources. */
private fun fakeCards(): List<Card> = (1..12).map { i ->
    Card(
        id = "card-$i",
        indonesian = "kata-$i",
        english = "word-$i",
        note = "note-$i",
    )
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], manifest = Config.NONE)
@OptIn(ExperimentalTestApi::class)
class DrillFlowTest {

    @Test
    fun answeringRevealsNoteAndAdvances() = runComposeUiTest {
        // Explicitly load the SQLite JDBC driver so it registers with DriverManager
        // even when running in Robolectric's isolated class loader context.
        Class.forName("org.sqlite.JDBC")
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val module = AppModule(
            content = ContentRepository(testCards = mapOf(TEST_MODULE to fakeCards())),
            progress = ProgressRepository(LancarDatabase(driver)),
            audio = NoopAudioPlayer(),
        )
        var finished = false
        setContent {
            DrillScreen(
                appModule = module,
                moduleId = TEST_MODULE,
                onFinish = { _, _, _ -> finished = true },
                onBack = {},
            )
        }
        waitForIdle()
        // Progress header must render for the first question (index 0, total = SESSION_SIZE = 12).
        onNodeWithText("1 / 12").assertExists()
    }
}
