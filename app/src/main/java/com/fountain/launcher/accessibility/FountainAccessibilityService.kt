package com.fountain.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.fountain.launcher.data.GatedAppRepository
import com.fountain.launcher.gate.GateActivity
import com.fountain.launcher.gate.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Detects the foreground app (spec §2.3, v2). Two jobs:
 *  - when a gated app comes forward with no active session, cover it with the gate
 *    (detect-then-cover — we can't intercept before it appears),
 *  - expose [kickHome] so the session-expiry alarm can force the user out with
 *    `GLOBAL_ACTION_HOME`, which works even deep inside another app.
 */
class FountainAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val recentPrompts = mutableMapOf<String, Long>()

    /** Per-package "you left this gated app" cancellations, pending the grace window. */
    private val pendingCancels = mutableMapOf<String, Job>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in IGNORED) return
        currentForeground = pkg
        scope.launch { onForeground(pkg) }
    }

    private suspend fun onForeground(pkg: String) {
        val manager = SessionManager(applicationContext)
        // Returning to a gated app cancels its pending "you left" timer, so a quick detour
        // out and back (e.g. back button, then reopen) keeps the same session instead of
        // re-prompting for a new duration.
        cancelPending(pkg)
        // Leaving a gated app doesn't stop its timer immediately — it schedules the stop
        // after LEAVE_GRACE_MS. Only a sustained departure actually cancels the session.
        scheduleLeaveCancels(pkg, manager)
        if (pkg == packageName) return // don't gate our own launcher/gate UI
        maybeGate(pkg, manager)
    }

    /** Schedule a delayed cancel for every active gated app the user just left. */
    private suspend fun scheduleLeaveCancels(currentPkg: String, manager: SessionManager) {
        for (session in manager.activeSessions()) {
            val left = session.packageName
            if (left == currentPkg) continue
            synchronized(pendingCancels) {
                if (pendingCancels.containsKey(left)) return@synchronized
                pendingCancels[left] = scope.launch {
                    delay(LEAVE_GRACE_MS)
                    // Still not back after the grace? Then it's a real departure — stop it.
                    if (currentForeground != left) {
                        SessionManager(applicationContext).endSession(left)
                    }
                    synchronized(pendingCancels) { pendingCancels.remove(left) }
                }
            }
        }
    }

    private fun cancelPending(pkg: String) {
        synchronized(pendingCancels) { pendingCancels.remove(pkg)?.cancel() }
    }

    private suspend fun maybeGate(pkg: String, manager: SessionManager) {
        val gatedRepo = GatedAppRepository(applicationContext)
        if (!gatedRepo.isGated(pkg)) return

        val now = System.currentTimeMillis()
        if (manager.activeSession(pkg, now) != null) return

        // Just kicked: the app closing emits a foreground event that would otherwise
        // re-prompt instantly. Bounce it home silently instead — this also blocks an
        // immediate un-gated reopen during the cooldown.
        if (manager.kickedWithin(pkg, KICK_COOLDOWN_MS, now)) {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            return
        }

        // Debounce: a single foregrounding can emit several window events.
        val last = recentPrompts[pkg] ?: 0L
        if (now - last < PROMPT_COOLDOWN_MS) return
        recentPrompts[pkg] = now

        GateActivity.openForForeground(applicationContext, pkg, resolveLabel(pkg))
    }

    private fun resolveLabel(pkg: String): String = try {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val PROMPT_COOLDOWN_MS = 1_500L
        private const val KICK_COOLDOWN_MS = 3_000L

        // How long a gated app can sit in the background before its timer is cancelled.
        // Short detours (back out, reopen) stay under this and keep the same session.
        private const val LEAVE_GRACE_MS = 30_000L
        private val IGNORED = setOf("com.android.systemui", "android")

        @Volatile
        private var instance: FountainAccessibilityService? = null

        @Volatile
        private var currentForeground: String? = null

        /** The package currently in the foreground, as last seen by the service. */
        fun currentForegroundPackage(): String? = currentForeground

        /** True if the kick was performed via the running service; false if unavailable. */
        fun kickHome(): Boolean =
            instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) ?: false
    }
}
