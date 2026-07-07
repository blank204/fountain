package com.fountain.launcher.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fountain.launcher.data.CapturedNotificationEntity
import com.fountain.launcher.data.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NotificationRepository(app)

    val inbox: StateFlow<List<CapturedNotificationEntity>> =
        repo.inbox.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onClear() {
        viewModelScope.launch { repo.clearInbox() }
    }
}
