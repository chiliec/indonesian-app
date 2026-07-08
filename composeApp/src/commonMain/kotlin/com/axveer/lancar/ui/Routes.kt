package com.axveer.lancar.ui

import kotlinx.serialization.Serializable

@Serializable object Home
@Serializable data class Drill(val moduleId: String)
@Serializable data class Results(
    val moduleId: String, val correct: Int, val total: Int, val newlyMastered: Int,
)
