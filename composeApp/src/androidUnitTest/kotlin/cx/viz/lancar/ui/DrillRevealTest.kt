package cx.viz.lancar.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

private const val REVEAL_MODULE = "reveal-module"

private class AudioContentRepository : ContentRepository() {
    private val fakes = (1..12).map { i ->
        Card(id = "card-$i", indonesian = "kata-$i", english = "word-$i", audio = "card-$i.m4a")
    }
    override suspend fun cards(moduleId: String): List<Card> =
        if (moduleId == REVEAL_MODULE) fakes else super.cards(moduleId)
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], manifest = Config.NONE)
@OptIn(ExperimentalTestApi::class)
class DrillRevealTest {

    @Test
    fun tappingShowWordRevealsIndonesian() = runComposeUiTest {
        Class.forName("org.sqlite.JDBC")
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        val module = AppModule(
            content = AudioContentRepository(),
            progress = ProgressRepository(db),
            settings = SettingsRepository(db),
            audio = NoopAudioPlayer(),
        )
        setContent {
            DrillScreen(
                appModule = module,
                moduleId = REVEAL_MODULE,
                onFinish = { _, _, _ -> },
                onBack = {},
            )
        }
        mainClock.advanceTimeBy(5_000)
        // On a LISTEN question the word is hidden; the reveal link is shown.
        onNodeWithText("Terlalu berisik? Lihat kata · Too loud? Show word").assertExists()
        onNodeWithText("Terlalu berisik? Lihat kata · Too loud? Show word").performClick()
        // After tapping, the first card's Indonesian text is revealed.
        onNodeWithText("kata-1").assertExists()
    }
}
