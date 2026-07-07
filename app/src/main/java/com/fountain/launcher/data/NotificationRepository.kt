package com.fountain.launcher.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Per-app notification rules and the captured inbox (spec §2.5). */
class NotificationRepository(context: Context) {

    private val db = FountainDatabase.get(context)
    private val ruleDao = db.notificationRuleDao()
    private val inboxDao = db.capturedNotificationDao()

    val rules: Flow<Map<String, NotificationMode>> =
        ruleDao.observeRules().map { list ->
            list.associate { it.packageName to NotificationMode.fromInt(it.mode) }
        }

    val inbox: Flow<List<CapturedNotificationEntity>> = inboxDao.observeAll()

    suspend fun modeFor(packageName: String): NotificationMode =
        NotificationMode.fromInt(ruleDao.modeFor(packageName) ?: 0)

    suspend fun setMode(packageName: String, mode: NotificationMode) =
        ruleDao.upsert(NotificationRuleEntity(packageName, mode.ordinal))

    suspend fun capture(entity: CapturedNotificationEntity) = inboxDao.insert(entity)

    suspend fun clearInbox() = inboxDao.clear()
}
