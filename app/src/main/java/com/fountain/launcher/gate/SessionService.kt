package com.fountain.launcher.gate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.fountain.launcher.R
import com.fountain.launcher.data.FountainDatabase
import com.fountain.launcher.data.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps Fountain alive while any time-gate session is running and shows the remaining
 * time in an ongoing notification. It does not own the countdown — the alarm does; this
 * only reflects the persisted end-times and stops itself once none remain.
 */
class SessionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dao by lazy { FountainDatabase.get(this).sessionDao() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification(emptyList(), System.currentTimeMillis()))
        scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val active = dao.getAllActive().filter { it.endEpochMs > now }
                if (active.isEmpty()) {
                    stopForegroundCompat()
                    stopSelf()
                    break
                }
                notify(buildNotification(active, now))
                delay(1_000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(active: List<SessionEntity>, now: Long): Notification {
        val soonest = active.minByOrNull { it.endEpochMs }
        val text = when {
            soonest == null -> "No active sessions"
            active.size == 1 -> "${remaining(soonest.endEpochMs - now)} left"
            else -> "${active.size} apps gated · ${remaining(soonest.endEpochMs - now)} left"
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Fountain")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun remaining(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0)) / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    private fun notify(notification: Notification) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active sessions",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shows time remaining on gated apps." }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    companion object {
        private const val CHANNEL_ID = "fountain_sessions"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, SessionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SessionService::class.java))
        }
    }
}
