package cx.viz.lancar.ui.onboarding

import cx.viz.lancar.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnboardingUiState(val step: Int = 0, val name: String = "")

class OnboardingViewModel(private val settings: SettingsRepository) {
    private val _state = MutableStateFlow(OnboardingUiState(name = settings.displayName().orEmpty()))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun next() { _state.value = _state.value.copy(step = (_state.value.step + 1).coerceAtMost(LAST_STEP)) }
    fun back() { _state.value = _state.value.copy(step = (_state.value.step - 1).coerceAtLeast(0)) }
    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v) }

    /** Persists name + onboarding-seen flag synchronously. */
    fun finish() {
        settings.setDisplayName(_state.value.name)
        settings.markOnboardingSeen()
    }

    companion object { const val LAST_STEP = 1 }
}
