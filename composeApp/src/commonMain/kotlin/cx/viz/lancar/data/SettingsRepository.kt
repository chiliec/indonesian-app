package cx.viz.lancar.data

import cx.viz.lancar.db.LancarDatabase

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

    fun showListenText(): Boolean = get(KEY_LISTEN_TEXT) == "true"
    fun setShowListenText(on: Boolean) = put(KEY_LISTEN_TEXT, if (on) "true" else "false")

    fun autoPlayAudio(): Boolean = get(KEY_AUTOPLAY) != "false"
    fun setAutoPlayAudio(on: Boolean) = put(KEY_AUTOPLAY, if (on) "true" else "false")

    private companion object {
        const val KEY_NAME = "display_name"
        const val KEY_ONBOARDED = "onboarding_seen"
        const val KEY_ACCENT = "accent"
        const val KEY_LISTEN_TEXT = "listen_show_text"
        const val KEY_AUTOPLAY = "autoplay_audio"
    }
}
