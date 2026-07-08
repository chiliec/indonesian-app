package com.axveer.lancar.domain

import kotlin.random.Random

class QuestionFactory(private val rng: Random = Random.Default) {

    fun build(card: Card, pool: List<Card>, isMastered: Boolean): Question {
        val mode = when {
            card.audio == null -> QuestionMode.TEXT
            isMastered && rng.nextInt(5) == 0 -> QuestionMode.PRODUCE
            else -> QuestionMode.LISTEN
        }
        val answerOf: (Card) -> String =
            if (mode == QuestionMode.PRODUCE) { c -> c.indonesian } else { c -> c.english }

        val correct = answerOf(card)
        val distractors = pool
            .asSequence()
            .filter { it.id != card.id }
            .map(answerOf)
            .filter { it != correct }
            .distinct()
            .toMutableList()
        shuffle(distractors)
        val chosen = distractors.take(3)

        // Insert correct answer at a random position and track the index directly,
        // avoiding indexOf() which can return the wrong index when distractors share
        // the same text as the correct answer.
        val options = chosen.toMutableList()
        val correctIndex = if (options.isEmpty()) 0 else rng.nextInt(options.size + 1)
        options.add(correctIndex, correct)

        val prompt = when (mode) {
            QuestionMode.LISTEN -> "What does this mean?"
            QuestionMode.TEXT -> "What does \"${card.indonesian}\" mean?"
            QuestionMode.PRODUCE -> "How do you say \"${card.english}\"?"
        }
        return Question(
            mode = mode,
            card = card,
            promptText = prompt,
            audio = if (mode == QuestionMode.LISTEN) card.audio else null,
            options = options,
            correctIndex = correctIndex,
        )
    }

    // Fisher-Yates using the injected rng (deterministic in tests).
    private fun <T> shuffle(list: MutableList<T>) {
        for (i in list.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        }
    }
}
