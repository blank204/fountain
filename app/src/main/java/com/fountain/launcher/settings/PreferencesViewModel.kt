package com.fountain.launcher.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fountain.launcher.data.FountainSettings
import com.fountain.launcher.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes the user-tunable settings and writes changes back to DataStore. */
class PreferencesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val settings: StateFlow<FountainSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FountainSettings())

    fun setReopenBreathing(enabled: Boolean) =
        launchEdit { repo.setReopenBreathingEnabled(enabled) }

    fun setReopenWindow(seconds: Int) =
        launchEdit { repo.setReopenWindowSeconds(seconds) }

    fun setIdleTimeout(seconds: Int) =
        launchEdit { repo.setIdleTimeoutSeconds(seconds) }

    fun setCrtOverlay(enabled: Boolean) =
        launchEdit { repo.setCrtOverlayEnabled(enabled) }

    fun setRequireUnlock(enabled: Boolean) =
        launchEdit { repo.setRequireUnlock(enabled) }

    private fun launchEdit(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
