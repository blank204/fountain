package com.fountain.launcher.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * One time-gate session, scoped to a package (spec §2.3). Timing is anchored to
 * [endEpochMs] — an absolute wall-clock instant — so it survives process death: on
 * restart we compare against `now`, never a lost in-memory countdown.
 *
 * A row persists after the session ends ([active] = false) so the reopen-breathing
 * window (spec §2.3) can measure time-since-kick without a second store.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val packageName: String,
    val startEpochMs: Long,
    val durationMs: Long,
    val endEpochMs: Long,
    val active: Boolean,
)

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE packageName = :packageName")
    suspend fun get(packageName: String): SessionEntity?

    @Query("UPDATE sessions SET active = 0 WHERE packageName = :packageName")
    suspend fun deactivate(packageName: String)

    @Query("SELECT * FROM sessions WHERE active = 1")
    suspend fun getAllActive(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE active = 1")
    fun observeActive(): Flow<List<SessionEntity>>
}
