package com.axveer.lancar.ui

import kotlinx.serialization.Serializable

@Serializable object Onboarding
@Serializable object Main
@Serializable data class Drill(val moduleId: String)
@Serializable data class Results(
    val moduleId: String, val correct: Int, val total: Int, val newlyMastered: Int,
)

/** First-run gate: onboarding until the user has completed it once, then the tab shell. */
fun startDestination(onboardingSeen: Boolean): Any = if (onboardingSeen) Main else Onboarding
