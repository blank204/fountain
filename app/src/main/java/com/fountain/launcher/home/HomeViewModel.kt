package com.fountain.launcher.home

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fountain.launcher.data.GatedAppRepository
import com.fountain.launcher.gate.GateActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val loading: Boolean = true,
    val query: String = "",
    /** Filtered, A–Z, grouped into (sectionKey -> apps) preserving order. */
    val sections: List<Pair<String, List<AppEntry>>> = emptyList(),
    /** Packages currently time-gated, for the long-press action label. */
    val gatedPackages: Set<String> = emptySet(),
    /** Pinned Phone/Camera shortcuts. */
    val quickApps: List<QuickApp> = emptyList(),
)

/**
 * Owns the launchable-app list and search state (unidirectional: UI sends intents in,
 * observes [uiState] out). Refreshes automatically when packages are installed/removed.
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val gatedRepo = GatedAppRepository(app)

    private val allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private val quickApps = MutableStateFlow<List<QuickApp>>(emptyList())
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> =
        combine(allApps, query, loading, gatedRepo.gatedPackages, quickApps) { apps, q, isLoading, gated, quick ->
            HomeUiState(
                loading = isLoading,
                query = q,
                sections = groupSections(apps, q),
                gatedPackages = gated,
                quickApps = quick,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refresh()
    }

    init {
        registerPackageReceiver(app)
        refresh()
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onLaunch(app: AppEntry) {
        viewModelScope.launch {
            if (gatedRepo.isGated(app.packageName)) {
                GateActivity.open(getApplication(), app.packageName, app.component, app.label)
            } else {
                repo.launch(app.component)
            }
        }
    }

    fun onOpenAppInfo(app: AppEntry) = repo.openAppInfo(app.packageName)

    fun onQuickLaunch(app: QuickApp) = repo.launchIntent(app.intent)

    fun onHide(app: AppEntry) {
        repo.setHidden(app.packageName, hidden = true)
        refresh()
    }

    fun onSetGated(app: AppEntry, gated: Boolean) {
        viewModelScope.launch { gatedRepo.setGated(app.packageName, gated) }
    }

    private fun refresh() {
        viewModelScope.launch {
            val (apps, quick) = withContext(Dispatchers.IO) {
                repo.loadApps() to repo.loadQuickApps()
            }
            allApps.value = apps
            quickApps.value = quick
            loading.value = false
        }
    }

    private fun registerPackageReceiver(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        // Package broadcasts are protected system broadcasts; NOT_EXPORTED is correct and
        // keeps registration valid on targetSdk 34 (Android 14) devices.
        ContextCompat.registerReceiver(
            context,
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(packageReceiver)
        super.onCleared()
    }

    private fun groupSections(
        apps: List<AppEntry>,
        query: String,
    ): List<Pair<String, List<AppEntry>>> {
        val filtered =
            if (query.isBlank()) apps
            else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
        return filtered
            .groupBy { it.sectionKey }
            .toSortedMap()
            .map { (key, list) -> key to list }
    }
}
