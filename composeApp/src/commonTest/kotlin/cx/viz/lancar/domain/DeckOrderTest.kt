package cx.viz.lancar.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DeckOrderTest {
    private fun cards(n: Int): List<Card> =
        (1..n).map { Card(id = "c$it", indonesian = "kata$it", english = "word$it") }

    @Test fun notShuffledReturnsInputOrder() {
        val src = cards(5)
        assertEquals(src, deckOrder(src, shuffled = false, rng = Random(1)))
    }

    @Test fun shuffledIsAPermutation() {
        val src = cards(20)
        val out = deckOrder(src, shuffled = true, rng = Random(1))
        assertEquals(src.toSet(), out.toSet())     // same elements
        assertEquals(src.size, out.size)           // none lost or duplicated
    }

    @Test fun sameSeedSameOrder() {
        val src = cards(20)
        assertEquals(
            deckOrder(src, shuffled = true, rng = Random(42)),
            deckOrder(src, shuffled = true, rng = Random(42)),
        )
    }

    @Test fun shuffleActuallyReorders() {
        val src = cards(20)
        assertNotEquals(src, deckOrder(src, shuffled = true, rng = Random(42)))
        assertTrue(src.size == 20)
    }
}
