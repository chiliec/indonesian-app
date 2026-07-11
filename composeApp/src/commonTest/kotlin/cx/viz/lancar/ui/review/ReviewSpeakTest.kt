package cx.viz.lancar.ui.review

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.MIXED_ID
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.Card
import cx.viz.lancar.domain.ModuleMeta
import cx.viz.lancar.platform.NoopAudioPlayer
import cx.viz.lancar.platform.SpeechRecognizer
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeContent : ContentRepository() {
    private val cards = listOf(
        Card(id = "c", indonesian = "makan", english = "eat"),
        Card(id = "d", indonesian = "minum", english = "drink"),
    )
    override suspend fun modules(): List<ModuleMeta> = listOf(ModuleMeta(MIXED_ID, "Mixed", 2))
    override suspend fun cards(moduleId: String): List<Card> = cards
}

private class FakeRecognizer(
    private val transcript: String?,
    private val available: Boolean = true,
) : SpeechRecognizer {
    override suspend fun isAvailable(): Boolean = available
    override suspend fun recognize(lang: String): String? = transcript
}

class ReviewSpeakTest {
    private fun module(recognizer: SpeechRecognizer): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) {}
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        var day = 10L
        val progress = ProgressRepository(db) { day }
        progress.recordAnswer("c", correct = true)
        day = 100L
        return AppModule(
            FakeContent(), progress, SettingsRepository(db), NoopAudioPlayer(),
            stt = recognizer,
        )
    }

    @Test fun availabilityReflectedInState() {
        val vm = ReviewViewModel(module(FakeRecognizer("makan")), dispatcher = Dispatchers.Unconfined)
        assertTrue(vm.state.value.sttAvailable)
    }

    @Test fun unavailableRecognizerHidesMic() {
        val vm = ReviewViewModel(
            module(FakeRecognizer(null, available = false)), dispatcher = Dispatchers.Unconfined,
        )
        assertFalse(vm.state.value.sttAvailable)
    }

    @Test fun matchingSpeechRecordsCorrect() {
        val vm = ReviewViewModel(module(FakeRecognizer("Makan")), dispatcher = Dispatchers.Unconfined)
        vm.onSpeak()
        val s = vm.state.value
        assertTrue(s.answered)
        assertEquals(s.question!!.correctIndex, s.selected)
        assertEquals(1, s.correctCount)
        assertNull(s.speechHint)
    }

    @Test fun mismatchedSpeechRecordsNothingAndHints() {
        val vm = ReviewViewModel(module(FakeRecognizer("zzz")), dispatcher = Dispatchers.Unconfined)
        vm.onSpeak()
        val s = vm.state.value
        assertFalse(s.answered)
        assertNull(s.selected)
        assertTrue(!s.speechHint.isNullOrBlank())
    }
}
