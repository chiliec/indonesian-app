package com.axveer.lancar.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

private fun card(id: String, indo: String, en: String, audio: String? = null) =
    Card(id = id, indonesian = indo, english = en, audio = audio)

class QuestionFactoryTest {
    private val pool = listOf(
        card("1", "satu", "one", "a.m4a"),
        card("2", "dua", "two", "b.m4a"),
        card("3", "tiga", "three", "c.m4a"),
        card("4", "empat", "four", "d.m4a"),
        card("5", "lima", "five", "e.m4a"),
    )

    @Test fun noAudioCardIsAlwaysText() {
        val c = card("x", "enam", "six", audio = null)
        val q = QuestionFactory(Random(1)).build(c, pool + c, isMastered = true)
        assertEquals(QuestionMode.TEXT, q.mode)
        assertEquals(null, q.audio)
    }

    @Test fun unmasteredAudioCardIsListen() {
        val q = QuestionFactory(Random(1)).build(pool[0], pool, isMastered = false)
        assertEquals(QuestionMode.LISTEN, q.mode)
        assertEquals("a.m4a", q.audio)
    }

    @Test fun masteredCardCanRollProduce() {
        // Use nextBits=0 override so nextInt(5)==0 deterministically → PRODUCE
        val rng = object : Random() { override fun nextBits(bitCount: Int) = 0 }
        val q = QuestionFactory(rng).build(pool[0], pool, isMastered = true)
        assertEquals(QuestionMode.PRODUCE, q.mode)
    }

    @Test fun alwaysFourDistinctOptionsWithCorrectAnswer() {
        val q = QuestionFactory(Random(7)).build(pool[0], pool, isMastered = false)
        assertEquals(4, q.options.size)
        assertEquals(q.options.size, q.options.toSet().size) // distinct
        assertEquals("one", q.options[q.correctIndex])        // LISTEN/TEXT -> english answer
    }

    @Test fun produceOptionsAreIndonesian() {
        // nextBits=0 → PRODUCE; correct answer must be the Indonesian form
        val rng = object : Random() { override fun nextBits(bitCount: Int) = 0 }
        val q = QuestionFactory(rng).build(pool[0], pool, isMastered = true)
        assertEquals(QuestionMode.PRODUCE, q.mode)
        assertEquals("satu", q.options[q.correctIndex])
    }
}
