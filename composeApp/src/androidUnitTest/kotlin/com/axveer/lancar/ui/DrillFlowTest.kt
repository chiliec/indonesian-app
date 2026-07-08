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

/**
 * Test-only subclass that overrides [cards] to return fake data for [TEST_MODULE],
 * avoiding any dependency on bundled Compose Multiplatform resources.
 */
private class FakeContentRepository : ContentRepository() {
    private val fakes = fakeCards()
    override suspend fun cards(moduleId: String): List<Card> =
        if (moduleId == TEST_MODULE) fakes else super.cards(moduleId)
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], manifest = Config.NONE)
@OptIn(ExperimentalTestApi::class)
class DrillFlowTest {

    @Test
    fun drillHeaderRendersOnLoad() = runComposeUiTest {
        // Robolectric runs in an isolated class loader; explicitly load the JDBC driver
        // so it registers with DriverManager before JdbcSqliteDriver initialises.
        Class.forName("org.sqlite.JDBC")
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val module = AppModule(
            content = FakeContentRepository(),
            progress = ProgressRepository(LancarDatabase(driver)),
            audio = NoopAudioPlayer(),
        )
        setContent {
            DrillScreen(
                appModule = module,
                moduleId = TEST_MODULE,
                onFinish = { _, _, _ -> },
                onBack = {},
            )
        }
        // Advance the clock to let the ViewModel's init coroutine (cards() + question build) run.
        mainClock.advanceTimeBy(5_000)
        // After cards load, the first answer option is visible — "word-1" through "word-12"
        // are the English labels used as multiple-choice options.
        onNodeWithText("word-1").assertExists()
    }
}
