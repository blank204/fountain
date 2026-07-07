package com.fountain.launcher.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A package the user has designated as time-gated (spec §2.3). */
@Entity(tableName = "gated_apps")
data class GatedApp(
    @PrimaryKey val packageName: String,
)

@Dao
interface GatedAppDao {
    @Query("SELECT packageName FROM gated_apps")
    fun observeGatedPackages(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM gated_apps WHERE packageName = :packageName)")
    suspend fun isGated(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(app: GatedApp)

    @Query("DELETE FROM gated_apps WHERE packageName = :packageName")
    suspend fun remove(packageName: String)
}
