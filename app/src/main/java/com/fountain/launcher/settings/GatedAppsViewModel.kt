package com.fountain.launcher.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fountain.launcher.data.GatedAppRepository
import com.fountain.launcher.home.AppEntry
import com.fountain.launcher.home.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GatedAppsUiState(
    val loading: Boolean = true,
    val query: String = "",
    val apps: List<AppEntry> = emptyList(),
    val gatedPackages: Set<String> = emptySet(),
)

/** Multi-select picker: designate any installed app as time-gated (spec §2.3). */
class GatedAppsViewModel(app: Application) : AndroidViewModel(app) {

    private val appRepo = AppRepository(app)
    private val gatedRepo = GatedAppRepository(app)

    private val allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<GatedAppsUiState> =
        combine(allApps, query, loading, gatedRepo.gatedPackages) { apps, q, isLoading, gated ->
            val filtered =
                if (q.isBlank()) apps
                else apps.filter { it.label.contains(q.trim(), ignoreCase = true) }
            GatedAppsUiState(
                loading = isLoading,
                query = q,
                apps = filtered,
                gatedPackages = gated,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GatedAppsUiState())

    init {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { appRepo.loadApps(includeHidden = true) }
            allApps.value = apps
            loading.value = false
        }
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onToggle(app: AppEntry, gated: Boolean) {
        viewModelScope.launch { gatedRepo.setGated(app.packageName, gated) }
    }
}
