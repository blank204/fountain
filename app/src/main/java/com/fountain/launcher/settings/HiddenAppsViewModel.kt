package com.fountain.launcher.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fountain.launcher.home.AppEntry
import com.fountain.launcher.home.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HiddenAppsUiState(
    val loading: Boolean = true,
    val apps: List<AppEntry> = emptyList(),
)

/** Lists hidden apps and lets the user restore them — the counterpart to home's Hide. */
class HiddenAppsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)

    private val _uiState = MutableStateFlow(HiddenAppsUiState())
    val uiState: StateFlow<HiddenAppsUiState> = _uiState

    init {
        refresh()
    }

    fun onUnhide(app: AppEntry) {
        repo.setHidden(app.packageName, hidden = false)
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { repo.loadHiddenApps() }
            _uiState.value = HiddenAppsUiState(loading = false, apps = apps)
        }
    }
}
