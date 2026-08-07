package cx.viz.lancar.ui.review

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.lancar.data.ContentRepository
import cx.viz.lancar.data.MIXED_ID
import cx.viz.lancar.data.ProgressRepository
import cx.viz.lancar.data.SettingsRepository
import cx.viz.lancar.db.LancarDatabase
import cx.viz.lancar.domain.Card
import cx.viz.lancar.domain.ModuleMeta
import cx.viz.lancar.domain.QuestionFactory
import cx.viz.lancar.platform.AudioPlayer
import cx.viz.lancar.platform.NoopAudioPlayer
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.drill.SLOW_RATE
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeReviewContent : ContentRepository() {
    private val m1 = listOf(
        Card(id = "a", indonesian = "satu", english = "one"),
        Card(id = "b", indonesian = "dua", english = "two"),
    )
    private val m2 = listOf(
        Card(id = "c", indonesian = "makan", english = "eat"),
        Card(id = "d", indonesian = "minum", english = "drink"),
    )
    override suspend fun modules(): List<ModuleMeta> = listOf(
        ModuleMeta(MIXED_ID, "Mixed", 4),
        ModuleMeta("m1", "M1", 2),
        ModuleMeta("m2", "M2", 2),
    )
    override suspend fun cards(moduleId: String): List<Card> = when (moduleId) {
        "m1" -> m1
        "m2" -> m2
        else -> m1 + m2 // MIXED
    }
}

private class RecordingAudio : AudioPlayer {
    val played = mutableListOf<Pair<String, Float>>()
    override suspend fun play(fileName: String, rate: Float): Long {
        played += fileName to rate
        return 0
    }
}

// Deterministic RNG that always forces LISTEN mode in QuestionFactory:
//   nextInt(5) [PRODUCE gate] → 1 ≠ 0 → LISTEN always chosen
//   nextInt(n) [shuffle / insert position] → safe value in [0, n)
// On JVM, nextInt(n) for non-power-of-2 calls nextBits(32) then ushr 1 then % n.
// nextBits(32) must return 2, not 0: `1 shl 32` wraps to 1 on JVM (shift mod 32),
// so saturating via (1 shl bitCount)-1 would yield 0 for bitCount=32. Hence the
// explicit 3-branch implementation below.
private val listenRng = object : kotlin.random.Random() {
    override fun nextBits(bitCount: Int): Int = when {
        bitCount <= 0 -> 0  // nextInt(1): fastLog2(1)=0 → returns 0 ✓
        bitCount == 1 -> 1  // nextInt(2) power-of-2 path: 1-bit value ✓
        else -> 2           // nextInt(5): nextBits(32)=2 → ushr1=1 → 1%5=1 ≠ 0 ✓
    }
}

// Due cards need audio for LISTEN mode to be reachable at all.
private class FakeAudioReviewContent : ContentRepository() {
    private val cards = listOf(
        Card(id = "a", indonesian = "satu", english = "one", audio = "a.m4a"),
        Card(id = "b", indonesian = "dua", english = "two", audio = "b.m4a"),
    )
    override suspend fun cards(moduleId: String): List<Card> = cards
}

class ReviewViewModelTest {
    private fun setup(today: () -> Long): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(FakeReviewContent(), ProgressRepository(db, today), SettingsRepository(db), NoopAudioPlayer())
    }

    private fun setup(content: ContentRepository, audio: AudioPlayer, today: () -> Long): AppModule {
        @Suppress("SwallowedException")
        try { Class.forName("org.sqlite.JDBC") } catch (_: ClassNotFoundException) { }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LancarDatabase.Schema.create(driver)
        val db = LancarDatabase(driver)
        return AppModule(content, ProgressRepository(db, today), SettingsRepository(db), audio,
            questionFactory = QuestionFactory(rng = listenRng))
    }

    @Test fun queueContainsOnlyDueCardsAcrossModules() {
        var day = 10L
        val module = setup { day }
        // Master one card from each module so both are scheduled.
        module.progress.recordAnswer("a", correct = true) // m1, due 11
        module.progress.recordAnswer("c", correct = true) // m2, due 11
        day = 100L
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        val s = vm.state.value
        assertTrue(s.question != null)
        assertEquals(2, s.total)
        // The current question's card is one of the two due cards.
        assertTrue(s.question!!.card.id in listOf("a", "c"))
    }

    @Test fun emptyWhenNothingDue() {
        val module = setup { 100L } // nothing mastered -> nothing scheduled
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        assertTrue(vm.state.value.finished)
        assertEquals(0, vm.state.value.total)
    }

    @Test fun seedsRevealTextFromSettings() {
        var day = 10L
        val module = setup { day }
        module.progress.recordAnswer("a", correct = true)
        day = 100L
        module.settings.setShowListenText(true)
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        assertTrue(vm.state.value.revealText)
    }

    @Test fun revealWordSetsFlag() {
        var day = 10L
        val module = setup { day }
        module.progress.recordAnswer("a", correct = true)
        day = 100L
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        assertFalse(vm.state.value.revealText)
        vm.toggleWord()
        assertTrue(vm.state.value.revealText)
    }

    @Test fun selectDoesNotGradeTheCard() {
        var day = 10L
        val module = setup { day }
        module.progress.recordAnswer("a", correct = true) // box 1, due day 11
        day = 100L
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        val before = module.progress.forCards(listOf("a"))["a"]!!
        vm.select(0)
        assertFalse(vm.state.value.answered)
        assertEquals(0, vm.state.value.selected)
        val after = module.progress.forCards(listOf("a"))["a"]!!
        assertEquals(before.correct, after.correct)
        assertEquals(before.wrong, after.wrong)
    }

    @Test fun checkGradesTheCardOnce() {
        var day = 10L
        val module = setup { day }
        module.progress.recordAnswer("a", correct = true)
        day = 100L
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        val q = vm.state.value.question!!
        val before = module.progress.forCards(listOf(q.card.id))[q.card.id]!!.correct
        vm.select(q.correctIndex)
        vm.check()
        vm.check()
        assertTrue(vm.state.value.answered)
        assertEquals(1, vm.state.value.correctCount)
        assertEquals(before + 1, module.progress.forCards(listOf(q.card.id))[q.card.id]!!.correct)
    }

    @Test fun slowPlaybackUsesReducedRate() {
        var day = 10L
        val audio = RecordingAudio()
        val module = setup(FakeAudioReviewContent(), audio) { day }
        module.progress.recordAnswer("a", correct = true)
        day = 100L
        module.settings.setAutoPlayAudio(false)
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        vm.playAudio(slow = true)
        assertEquals(listOf("a.m4a" to SLOW_RATE), audio.played)
    }

    @Test fun autoPlayMatchesEmittedModeWhenOn() {
        var day = 10L
        val audio = RecordingAudio()
        val module = setup(FakeAudioReviewContent(), audio) { day } // autoPlay defaults on, listenRng forces LISTEN
        module.progress.recordAnswer("a", correct = true)
        day = 100L
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        assertEquals(listOf("a.m4a" to 1f), audio.played) // LISTEN guaranteed by listenRng
    }

    @Test fun autoPlaySilentWhenOff() {
        var day = 10L
        val audio = RecordingAudio()
        val module = setup(FakeAudioReviewContent(), audio) { day }
        module.progress.recordAnswer("a", correct = true)
        day = 100L
        module.settings.setAutoPlayAudio(false)
        val vm = ReviewViewModel(module, dispatcher = Dispatchers.Unconfined)
        assertTrue(vm.state.value.question != null)
        assertTrue(audio.played.isEmpty())
    }
}
