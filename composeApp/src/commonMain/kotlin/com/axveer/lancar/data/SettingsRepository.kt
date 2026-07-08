package com.axveer.lancar.data

import com.axveer.lancar.db.LancarDatabase

class SettingsRepository(db: LancarDatabase) {
    private val q = db.settingsQueries

    private fun get(key: String): String? = q.get(key).executeAsOneOrNull()
    private fun put(key: String, value: String) = q.upsert(key = key, value = value)

    fun displayName(): String? = get(KEY_NAME)?.takeIf { it.isNotBlank() }
    fun setDisplayName(name: String?) = put(KEY_NAME, name?.trim().orEmpty())

    fun onboardingSeen(): Boolean = get(KEY_ONBOARDED) == "true"
    fun markOnboardingSeen() = put(KEY_ONBOARDED, "true")

    fun accentName(): String? = get(KEY_ACCENT)
    fun setAccentName(name: String) = put(KEY_ACCENT, name)

    private companion object {
        const val KEY_NAME = "display_name"
        const val KEY_ONBOARDED = "onboarding_seen"
        const val KEY_ACCENT = "accent"
    }
}
