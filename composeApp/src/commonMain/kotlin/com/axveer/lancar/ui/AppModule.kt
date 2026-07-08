package com.axveer.lancar.ui

import com.axveer.lancar.data.ContentRepository
import com.axveer.lancar.data.ProgressRepository
import com.axveer.lancar.data.SettingsRepository
import com.axveer.lancar.domain.QuestionFactory
import com.axveer.lancar.platform.AudioPlayer
import com.axveer.lancar.ui.theme.Accent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppModule(
    val content: ContentRepository,
    val progress: ProgressRepository,
    val settings: SettingsRepository,
    val audio: AudioPlayer,
) {
    val questionFactory = QuestionFactory()

    private val _accent = MutableStateFlow(Accent.fromName(settings.accentName()))
    val accent: StateFlow<Accent> = _accent.asStateFlow()

    fun setAccent(a: Accent) {
        settings.setAccentName(a.name)
        _accent.value = a
    }
}
