package com.fountain.launcher.notifications

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.fountain.launcher.data.CapturedNotificationEntity
import com.fountain.launcher.data.NotificationMode
import com.fountain.launcher.data.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Captures posted notifications and applies each app's rule (spec §2.5). Suppress and
 * "show in inbox" are necessarily post-then-cancel: we can only act after the system
 * posts, so the origin app's first heads-up/sound may already have fired.
 */
class FountainNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repo: NotificationRepository
    private var sound: PixelSound? = null

    override fun onCreate() {
        super.onCreate()
        repo = NotificationRepository(applicationContext)
        sound = PixelSound(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName == packageName) return
        // Leave persistent/ongoing notifications (music, our own service) alone.
        if (notification.isOngoing) return

        scope.launch {
            when (repo.modeFor(notification.packageName)) {
                NotificationMode.PASSTHROUGH -> Unit
                NotificationMode.SUPPRESS -> cancelNotification(notification.key)
                NotificationMode.SHOW -> {
                    repo.capture(notification.toEntity())
                    cancelNotification(notification.key)
                    sound?.play()
                }
            }
        }
    }

    private fun StatusBarNotification.toEntity(): CapturedNotificationEntity {
        val extras = notification.extras
        return CapturedNotificationEntity(
            packageName = packageName,
            appLabel = resolveLabel(packageName),
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            postedAtEpochMs = postTime,
        )
    }

    private fun resolveLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    override fun onDestroy() {
        scope.cancel()
        sound?.release()
        super.onDestroy()
    }
}
