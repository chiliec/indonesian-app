package cx.viz.lancar.ui.drill

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.Card
import cx.viz.lancar.platform.AudioPlayer
import cx.viz.lancar.platform.NoopAudioPlayer
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TEST_MODULE = "test-module"

private class FakeContent : ContentRepository() {
    private val cards = (1..12).map { i ->
        Card(id = "c-$i", indonesian = "kata-$i", english = "word-$i", audio = "c-$i.m4a")
    }
    override suspend fun cards(moduleId: String): List<Card> =
        if (moduleId == TEST_MODULE) cards else super.cards(moduleId)
}

private class RecordingAudio : AudioPlayer {
    val played = mutableListOf<Pair<String, Float>>()
    override suspend fun play(fileName: String, rate: Float): Long {
        played += fileName to rate
        return 0
    }
}

// Cards without audio -> QuestionFactory yields TEXT mode (never LISTEN).
private class FakeNoAudioContent : ContentRepository() {
    private val cards = (1..12).map { i ->
        Card(id = "c-$i", indonesian = "kata-$i", english = "word-$i")
    }
    override suspend fun cards(moduleId: String): List<Card> =
        if (moduleId == TEST_MODULE) cards else super.cards(moduleId)
}

class DrillViewModelTest {
    private fun module(): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(FakeContent(), ProgressRepository(db), SettingsRepository(db), NoopAudioPlayer())
    }

    private fun module(content: ContentRepository, audio: AudioPlayer): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(content, ProgressRepository(db), SettingsRepository(db), audio)
    }

    // DrillViewModel hops to Dispatchers.Default inside init/emitQuestion, so emission
    // is async even under Unconfined. Poll with a bounded wait.
    private fun waitFor(cond: () -> Boolean) {
        repeat(200) { if (cond()) return; Thread.sleep(10) }
    }

    @Test fun seedsRevealTextFromSettings() {
        val m = module()
        m.settings.setShowListenText(true)
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        assertTrue(vm.state.value.revealText)
    }

    @Test fun revealWordSetsFlag() {
        val m = module() // setting defaults false
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        assertFalse(vm.state.value.revealText)
        vm.toggleWord()
        assertTrue(vm.state.value.revealText)
    }

    @Test fun selectRecordsNothing() {
        val m = module()
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        waitFor { vm.state.value.question != null }
        val cardId = vm.state.value.question!!.card.id
        vm.select(0)
        assertEquals(0, vm.state.value.selected)
        assertFalse(vm.state.value.answered)
        assertTrue(m.progress.forCards(listOf(cardId)).isEmpty())
        vm.dispose()
    }

    @Test fun reselectingOverwritesSelection() {
        val m = module()
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        waitFor { vm.state.value.question != null }
        vm.select(0)
        vm.select(2)
        assertEquals(2, vm.state.value.selected)
        assertFalse(vm.state.value.answered)
        vm.dispose()
    }

    @Test fun checkCommitsOnceAndIsIdempotent() {
        val m = module()
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        waitFor { vm.state.value.question != null }
        val q = vm.state.value.question!!
        vm.select(q.correctIndex)
        vm.check()
        vm.check() // second call must be a no-op
        val s = vm.state.value
        assertTrue(s.answered)
        assertEquals(1, s.correctCount)
        assertEquals(1, m.progress.forCards(listOf(q.card.id))[q.card.id]!!.correct)
        vm.dispose()
    }

    @Test fun checkWithoutSelectionDoesNothing() {
        val m = module()
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        waitFor { vm.state.value.question != null }
        val cardId = vm.state.value.question!!.card.id
        vm.check()
        assertFalse(vm.state.value.answered)
        assertTrue(m.progress.forCards(listOf(cardId)).isEmpty())
        vm.dispose()
    }

    @Test fun toggleWordFlipsBothWays() {
        val m = module() // showListenText defaults false
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        assertFalse(vm.state.value.revealText)
        vm.toggleWord()
        assertTrue(vm.state.value.revealText)
        vm.toggleWord()
        assertFalse(vm.state.value.revealText)
        vm.dispose()
    }

    @Test fun slowPlaybackUsesReducedRate() {
        val audio = RecordingAudio()
        val m = module(FakeContent(), audio)
        m.settings.setAutoPlayAudio(false) // keep the recording clean
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        waitFor { vm.state.value.question != null }
        vm.playAudio(slow = true)
        waitFor { audio.played.isNotEmpty() }
        assertEquals(listOf("c-1.m4a" to SLOW_RATE), audio.played)
        vm.dispose()
    }

    @Test fun normalPlaybackUsesFullRate() {
        val audio = RecordingAudio()
        val m = module(FakeContent(), audio)
        m.settings.setAutoPlayAudio(false)
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        waitFor { vm.state.value.question != null }
        vm.playAudio()
        waitFor { audio.played.isNotEmpty() }
        assertEquals(listOf("c-1.m4a" to 1f), audio.played)
        assertFalse(vm.state.value.playing) // fake returns duration 0 -> clears immediately
        vm.dispose()
    }

    @Test fun autoPlayPlaysListenQuestionWhenOn() {
        val audio = RecordingAudio()
        val m = module(FakeContent(), audio) // autoPlay defaults on; cards have audio -> LISTEN
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        waitFor { audio.played.isNotEmpty() }
        assertEquals(listOf("c-1.m4a" to 1f), audio.played)
        vm.dispose()
    }

    @Test fun autoPlaySilentWhenOff() {
        val audio = RecordingAudio()
        val m = module(FakeContent(), audio)
        m.settings.setAutoPlayAudio(false)
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        waitFor { vm.state.value.question != null } // wait until first question emitted
        assertTrue(audio.played.isEmpty())
        vm.dispose()
    }

    @Test fun autoPlaySkipsNonListenQuestion() {
        val audio = RecordingAudio()
        val m = module(FakeNoAudioContent(), audio) // TEXT mode, autoPlay on
        val vm = DrillViewModel(m, TEST_MODULE, dispatcher = Dispatchers.Unconfined)
        waitFor { vm.state.value.question != null }
        assertTrue(audio.played.isEmpty())
        vm.dispose()
    }
}
