package com.axveer.lancar.ui.profile

import com.axveer.lancar.ui.AppModule
import com.axveer.lancar.ui.theme.Accent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(val name: String, val accent: Accent)

class ProfileViewModel(private val module: AppModule) {
    private val _state = MutableStateFlow(
        ProfileUiState(name = module.settings.displayName().orEmpty(), accent = module.accent.value),
    )
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun setName(name: String) {
        module.settings.setDisplayName(name)
        _state.value = _state.value.copy(name = module.settings.displayName().orEmpty())
    }

    fun setAccent(a: Accent) {
        module.setAccent(a)
        _state.value = _state.value.copy(accent = a)
    }

    fun resetProgress() = module.progress.reset()
}
