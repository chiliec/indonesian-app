package com.axveer.lancar.domain

import kotlinx.serialization.Serializable

enum class QuestionMode { LISTEN, TEXT, PRODUCE }

@Serializable
data class Sentence(val text: String, val blank: String, val en: String)

@Serializable
data class Card(
    val id: String,
    val indonesian: String,
    val english: String,
    val note: String? = null,
    val audio: String? = null,
    val sentences: List<Sentence> = emptyList(),
)

data class ModuleMeta(val id: String, val title: String, val cardCount: Int)
data class Module(val meta: ModuleMeta, val cards: List<Card>)

data class Question(
    val mode: QuestionMode,
    val card: Card,
    val promptText: String,
    val audio: String?,
    val options: List<String>,
    val correctIndex: Int,
)

data class CardProgress(
    val cardId: String,
    val seen: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
)
