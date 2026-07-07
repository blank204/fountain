package com.fountain.launcher.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** How Fountain treats an app's notifications (spec §2.5). */
enum class NotificationMode {
    PASSTHROUGH, // leave the native notification untouched (default)
    SHOW,        // capture into our inbox + play the pixel pop, remove from the shade
    SUPPRESS;    // remove it, don't keep it

    companion object {
        fun fromInt(value: Int): NotificationMode = entries.getOrElse(value) { PASSTHROUGH }
    }
}

@Entity(tableName = "notification_rules")
data class NotificationRuleEntity(
    @PrimaryKey val packageName: String,
    val mode: Int,
)

@Entity(tableName = "captured_notifications")
data class CapturedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAtEpochMs: Long,
)

@Dao
interface NotificationRuleDao {
    @Query("SELECT * FROM notification_rules")
    fun observeRules(): Flow<List<NotificationRuleEntity>>

    @Query("SELECT mode FROM notification_rules WHERE packageName = :packageName")
    suspend fun modeFor(packageName: String): Int?

    @Upsert
    suspend fun upsert(rule: NotificationRuleEntity)
}

@Dao
interface CapturedNotificationDao {
    @Query("SELECT * FROM captured_notifications ORDER BY postedAtEpochMs DESC")
    fun observeAll(): Flow<List<CapturedNotificationEntity>>

    @Insert
    suspend fun insert(notification: CapturedNotificationEntity)

    @Query("DELETE FROM captured_notifications")
    suspend fun clear()
}
