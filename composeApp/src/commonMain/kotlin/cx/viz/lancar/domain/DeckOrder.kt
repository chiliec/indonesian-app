package cx.viz.lancar.domain

import kotlin.random.Random

/** Returns [cards] in module order, or a shuffled permutation using [rng]. Pure. */
fun deckOrder(cards: List<Card>, shuffled: Boolean, rng: Random): List<Card> =
    if (shuffled) cards.shuffled(rng) else cards
