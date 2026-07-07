package com.fountain.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fountain_settings")

/** User-tunable settings. Defaults follow the spec; later milestones read these. */
data class FountainSettings(
    val reopenBreathingEnabled: Boolean = false,   // spec §2.3: default OFF
    val reopenWindowSeconds: Int = 10,             // grace window after a kick
    val idleTimeoutSeconds: Int = 60,              // spec §2.4 lock idle-on-home
    val crtOverlayEnabled: Boolean = true,         // spec §2.6: default ON
    val requireUnlock: Boolean = false,            // spec §2.4: use the device credential to unlock
    val onboardingSeen: Boolean = false,           // first-run setup completed
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<FountainSettings> = context.dataStore.data.map { p ->
        FountainSettings(
            reopenBreathingEnabled = p[KEY_REOPEN_BREATHING] ?: false,
            reopenWindowSeconds = p[KEY_REOPEN_WINDOW] ?: 10,
            idleTimeoutSeconds = p[KEY_IDLE_TIMEOUT] ?: 60,
            crtOverlayEnabled = p[KEY_CRT_OVERLAY] ?: true,
            requireUnlock = p[KEY_REQUIRE_UNLOCK] ?: false,
            onboardingSeen = p[KEY_ONBOARDING_SEEN] ?: false,
        )
    }

    suspend fun setReopenBreathingEnabled(enabled: Boolean) =
        edit { it[KEY_REOPEN_BREATHING] = enabled }

    suspend fun setReopenWindowSeconds(seconds: Int) =
        edit { it[KEY_REOPEN_WINDOW] = seconds }

    suspend fun setIdleTimeoutSeconds(seconds: Int) =
        edit { it[KEY_IDLE_TIMEOUT] = seconds }

    suspend fun setCrtOverlayEnabled(enabled: Boolean) =
        edit { it[KEY_CRT_OVERLAY] = enabled }

    suspend fun setRequireUnlock(enabled: Boolean) =
        edit { it[KEY_REQUIRE_UNLOCK] = enabled }

    suspend fun setOnboardingSeen(seen: Boolean) =
        edit { it[KEY_ONBOARDING_SEEN] = seen }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        val KEY_REOPEN_BREATHING = booleanPreferencesKey("reopen_breathing_enabled")
        val KEY_REOPEN_WINDOW = intPreferencesKey("reopen_window_seconds")
        val KEY_IDLE_TIMEOUT = intPreferencesKey("idle_timeout_seconds")
        val KEY_CRT_OVERLAY = booleanPreferencesKey("crt_overlay_enabled")
        val KEY_REQUIRE_UNLOCK = booleanPreferencesKey("require_unlock")
        val KEY_ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    }
}
