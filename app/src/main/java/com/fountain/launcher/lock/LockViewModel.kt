package com.fountain.launcher.lock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fountain.launcher.data.CapturedNotificationEntity
import com.fountain.launcher.data.NotificationRepository
import com.fountain.launcher.home.AppRepository
import com.fountain.launcher.home.QuickApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Feeds the fountain/lock screen its notifications and the Phone/Camera shortcuts. */
class LockViewModel(app: Application) : AndroidViewModel(app) {

    private val notifRepo = NotificationRepository(app)
    private val appRepo = AppRepository(app)

    val topNotifications: StateFlow<List<CapturedNotificationEntity>> =
        notifRepo.inbox.map { it.take(3) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _quickApps = MutableStateFlow<List<QuickApp>>(emptyList())
    val quickApps: StateFlow<List<QuickApp>> = _quickApps

    init {
        viewModelScope.launch {
            _quickApps.value = withContext(Dispatchers.IO) { appRepo.loadQuickApps() }
        }
    }

    fun onQuickLaunch(app: QuickApp) = appRepo.launchIntent(app.intent)
}
