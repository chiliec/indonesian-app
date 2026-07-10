package cx.viz.lancar.ui.kartu

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.Card
import cx.viz.lancar.platform.AudioPlayer
import cx.viz.lancar.ui.AppModule
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val M = "module-1"

private class FakeDeckContent : ContentRepository() {
    val fakes = (1..20).map { Card(id = "c$it", indonesian = "kata$it", english = "word$it") }
    override suspend fun cards(moduleId: String): List<Card> =
        if (moduleId == M) fakes else emptyList()
}

private class RecordingAudio : AudioPlayer {
    val played = mutableListOf<String>()
    override suspend fun play(fileName: String) { played += fileName }
}

class CardDeckViewModelTest {
    private fun module(content: ContentRepository, audio: AudioPlayer): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(content, ProgressRepository(db), SettingsRepository(db), audio)
    }

    @Test fun loadsCardsInModuleOrder() {
        val content = FakeDeckContent()
        val vm = CardDeckViewModel(module(content, RecordingAudio()), M,
            rng = Random(1), dispatcher = Dispatchers.Unconfined)
        val s = vm.state.value
        assertFalse(s.loading)
        assertFalse(s.shuffled)
        assertEquals(content.fakes, s.cards)
    }

    @Test fun toggleShuffleReordersThenRestores() {
        val content = FakeDeckContent()
        val vm = CardDeckViewModel(module(content, RecordingAudio()), M,
            rng = Random(42), dispatcher = Dispatchers.Unconfined)

        vm.toggleShuffle()
        assertTrue(vm.state.value.shuffled)
        assertEquals(content.fakes.toSet(), vm.state.value.cards.toSet()) // same elements
        // With seed 42 the 20-card order differs from source.
        assertTrue(vm.state.value.cards != content.fakes)

        vm.toggleShuffle()
        assertFalse(vm.state.value.shuffled)
        assertEquals(content.fakes, vm.state.value.cards) // exact module order restored
    }

    @Test fun playAudioDelegatesToPlayer() {
        val audio = RecordingAudio()
        val vm = CardDeckViewModel(module(FakeDeckContent(), audio), M,
            dispatcher = Dispatchers.Unconfined)
        vm.playAudio("abc.m4a")
        assertEquals(listOf("abc.m4a"), audio.played)
    }
}
