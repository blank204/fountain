package com.fountain.launcher.gate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.fountain.launcher.data.FountainDatabase
import com.fountain.launcher.data.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinates a time-gate session's three durable pieces (spec §2.3, v2):
 *  1. an absolute end-time persisted in Room (survives process death),
 *  2. an exact [AlarmManager] alarm that fires the kick at that instant,
 *  3. the foreground [SessionService] that keeps us alive and shows remaining time.
 *
 * Nothing here relies on an in-memory countdown.
 */
class SessionManager(private val context: Context) {

    private val dao = FountainDatabase.get(context).sessionDao()
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Start (or restart) a session for [packageName] lasting [durationMs]. */
    suspend fun startSession(packageName: String, durationMs: Long, now: Long) {
        val end = now + durationMs
        dao.upsert(
            SessionEntity(
                packageName = packageName,
                startEpochMs = now,
                durationMs = durationMs,
                endEpochMs = end,
                active = true,
            )
        )
        scheduleAlarm(packageName, end)
        SessionService.start(context)
    }

    /** End a session (expiry or manual). Keeps the row so the reopen window can read it. */
    suspend fun endSession(packageName: String) {
        dao.deactivate(packageName)
        cancelAlarm(packageName)
        if (dao.getAllActive().isEmpty()) SessionService.stop(context)
    }

    /** Every currently-active session. The service decides which ones to end on leave. */
    suspend fun activeSessions(): List<SessionEntity> = dao.getAllActive()

    /**
     * Live set of active-session packages. The accessibility service collects this into an
     * in-memory cache so it can decide leave-cancels without a DB read on every app switch.
     * Room stays the source of truth — this is a read cache fed by the same table.
     */
    val activeSessionPackages: Flow<Set<String>> =
        dao.observeActive().map { sessions -> sessions.mapTo(HashSet()) { it.packageName } }

    /**
     * Single-read gate decision for [packageName], folding what [activeSession] and
     * [kickedWithin] would each read separately into one row fetch.
     */
    suspend fun gateState(packageName: String, kickWindowMs: Long, now: Long): GateState {
        val session = dao.get(packageName) ?: return GateState.NONE
        return when {
            session.active && session.endEpochMs > now -> GateState.ACTIVE
            !session.active && now - session.endEpochMs in 0..kickWindowMs -> GateState.RECENTLY_KICKED
            else -> GateState.NONE
        }
    }

    /** An active, not-yet-expired session for this package, or null. */
    suspend fun activeSession(packageName: String, now: Long): SessionEntity? =
        dao.get(packageName)?.takeIf { it.active && it.endEpochMs > now }

    /** True if the app was kicked within [windowMs] — used for the reopen breathing screen. */
    suspend fun kickedWithin(packageName: String, windowMs: Long, now: Long): Boolean =
        dao.get(packageName)?.let { !it.active && now - it.endEpochMs in 0..windowMs } ?: false

    /**
     * Reconcile persisted sessions against wall-clock time. Call on boot and service
     * restart: expired sessions are ended; still-active ones get their alarm re-armed.
     */
    suspend fun reconcile(now: Long) {
        val active = dao.getAllActive()
        for (session in active) {
            if (session.endEpochMs <= now) endSession(session.packageName)
            else scheduleAlarm(session.packageName, session.endEpochMs)
        }
        if (dao.getAllActive().isEmpty()) SessionService.stop(context)
        else SessionService.start(context)
    }

    private fun scheduleAlarm(packageName: String, endEpochMs: Long) {
        val pi = expiryPendingIntent(packageName)
        // If exact alarms are denied (12+), fall back to inexact — the kick may lag under
        // Doze; onboarding (M5/M9) walks the user to grant exact-alarm access.
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endEpochMs, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endEpochMs, pi)
        }
    }

    private fun cancelAlarm(packageName: String) {
        alarmManager.cancel(expiryPendingIntent(packageName))
    }

    private fun expiryPendingIntent(packageName: String): PendingIntent {
        val intent = Intent(context, SessionExpiryReceiver::class.java).apply {
            action = ACTION_SESSION_EXPIRED
            putExtra(EXTRA_PACKAGE, packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            packageName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Outcome of [gateState]: is this gated app already open, just kicked, or fresh? */
    enum class GateState { ACTIVE, RECENTLY_KICKED, NONE }

    companion object {
        const val ACTION_SESSION_EXPIRED = "com.fountain.launcher.SESSION_EXPIRED"
        const val EXTRA_PACKAGE = "package"
    }
}
