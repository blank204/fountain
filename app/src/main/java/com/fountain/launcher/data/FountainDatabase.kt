package com.fountain.launcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local Room database. All data is on-device — no accounts, no network (spec §1).
 *
 * New entities (sessions in M4, captured notifications in M6) are added by bumping
 * [VERSION]; debug builds use destructive migration since there is no released schema yet.
 */
@Database(
    entities = [
        GatedApp::class,
        SessionEntity::class,
        NotificationRuleEntity::class,
        CapturedNotificationEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class FountainDatabase : RoomDatabase() {
    abstract fun gatedAppDao(): GatedAppDao
    abstract fun sessionDao(): SessionDao
    abstract fun notificationRuleDao(): NotificationRuleDao
    abstract fun capturedNotificationDao(): CapturedNotificationDao

    companion object {
        @Volatile
        private var instance: FountainDatabase? = null

        fun get(context: Context): FountainDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FountainDatabase::class.java,
                    "fountain.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
