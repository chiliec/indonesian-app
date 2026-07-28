package cx.viz.lancar.ui.profile

import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.theme.Accent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(
    val name: String,
    val accent: Accent,
    val showListenText: Boolean,
    val autoPlay: Boolean,
)

class ProfileViewModel(private val module: AppModule) {
    private val _state = MutableStateFlow(
        ProfileUiState(
            name = module.settings.displayName().orEmpty(),
            accent = module.accent.value,
            showListenText = module.settings.showListenText(),
            autoPlay = module.settings.autoPlayAudio(),
        ),
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

    fun setShowListenText(on: Boolean) {
        module.settings.setShowListenText(on)
        _state.value = _state.value.copy(showListenText = on)
    }

    fun setAutoPlay(on: Boolean) {
        module.settings.setAutoPlayAudio(on)
        _state.value = _state.value.copy(autoPlay = on)
    }

    fun resetProgress() = module.progress.reset()
}
