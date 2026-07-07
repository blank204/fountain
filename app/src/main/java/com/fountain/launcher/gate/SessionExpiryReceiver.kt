package com.fountain.launcher.gate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fountain.launcher.accessibility.FountainAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires when a session's exact alarm goes off. Ends the session and sends the user home.
 *
 * M4 uses the home-intent redirect (legitimate because Fountain is the launcher). M5's
 * AccessibilityService upgrades this to `GLOBAL_ACTION_HOME`, which also works when the
 * user is deep inside another app where a background activity start would be blocked.
 */
class SessionExpiryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SessionManager.ACTION_SESSION_EXPIRED) return
        val packageName = intent.getStringExtra(SessionManager.EXTRA_PACKAGE) ?: return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                SessionManager(appContext).endSession(packageName)
                // Only kick if the gated app is STILL the foreground app. If the user
                // already left it (e.g. moved to another app), don't yank them out of
                // whatever they're now using — that was the "it minimised Claude" bug.
                val foreground = FountainAccessibilityService.currentForegroundPackage()
                if (foreground == packageName) {
                    if (!FountainAccessibilityService.kickHome()) goHome(appContext)
                }
                // Reopening the app re-gates it — that's the enforcement. We don't kill the
                // process (unreliable on Samsung, leaves a suspended state; true force-stop
                // needs root/device-owner).
            } finally {
                pending.finish()
            }
        }
    }

    private fun goHome(context: Context) {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(home)
    }
}

