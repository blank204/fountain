package com.fountain.launcher.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fountain.launcher.data.NotificationMode
import com.fountain.launcher.data.NotificationRepository
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

data class NotificationRulesUiState(
    val loading: Boolean = true,
    val query: String = "",
    val apps: List<AppEntry> = emptyList(),
    val rules: Map<String, NotificationMode> = emptyMap(),
)

/** Per-app notification mode picker (spec §2.5). Default mode is passthrough. */
class NotificationRulesViewModel(app: Application) : AndroidViewModel(app) {

    private val appRepo = AppRepository(app)
    private val notifRepo = NotificationRepository(app)

    private val allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<NotificationRulesUiState> =
        combine(allApps, query, loading, notifRepo.rules) { apps, q, isLoading, rules ->
            val filtered =
                if (q.isBlank()) apps
                else apps.filter { it.label.contains(q.trim(), ignoreCase = true) }
            NotificationRulesUiState(isLoading, q, filtered, rules)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationRulesUiState())

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

    fun onSetMode(app: AppEntry, mode: NotificationMode) {
        viewModelScope.launch { notifRepo.setMode(app.packageName, mode) }
    }
}
