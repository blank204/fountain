package com.fountain.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.fountain.launcher.data.GatedAppRepository
import com.fountain.launcher.gate.GateActivity
import com.fountain.launcher.gate.SessionManager
import com.fountain.launcher.gate.SessionManager.GateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    /** Cache of "is this package a real launchable app" — avoids repeat PackageManager calls. */
    private val launchableCache = mutableMapOf<String, Boolean>()

    // One instance each instead of allocating per foreground event; both are cheap wrappers
    // over the singleton Room DB.
    private val sessionManager by lazy { SessionManager(applicationContext) }
    private val gatedRepo by lazy { GatedAppRepository(applicationContext) }

    // In-memory read caches fed by Room Flows, so the per-event hot path stays off the DB.
    // Room remains the source of truth — these only mirror it.
    @Volatile
    private var gatedPackages: Set<String> = emptySet()

    @Volatile
    private var activeSessionPackages: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        gatedRepo.gatedPackages
            .onEach { gatedPackages = it }
            .launchIn(scope)
        sessionManager.activeSessionPackages
            .onEach { activeSessionPackages = it }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Only real app switches move the foreground. Transient overlays — the soft keyboard,
        // dialogs, autofill, share sheets, system prompts — fire window-state-changed with
        // their own package but don't mean the user left the gated app. Treating them as a
        // departure is what let a session's timer reset mid-use (e.g. while typing): the
        // leave-grace job saw currentForeground != the gated app and ended the session.
        if (!isRealForegroundApp(pkg)) return
        currentForeground = pkg
        scope.launch { onForeground(pkg) }
    }

    /**
     * True if [pkg] is a real, launchable app the user actually switched to. Overlay/system
     * packages (most IMEs, dialogs, `gms`, the permission UI) have no launcher entry, so
     * they're filtered out. Erring here only risks *not* detecting a leave — the session
     * runs a little longer — never a spurious mid-session reset.
     */
    private fun isRealForegroundApp(pkg: String): Boolean {
        if (pkg in IGNORED) return false
        if (pkg == packageName) return true // our own launcher/home is a legitimate surface
        return launchableCache.getOrPut(pkg) {
            packageManager.getLaunchIntentForPackage(pkg) != null
        }
    }

    private suspend fun onForeground(pkg: String) {
        // Returning to a gated app cancels its pending "you left" timer, so a quick detour
        // out and back (e.g. back button, then reopen) keeps the same session instead of
        // re-prompting for a new duration.
        cancelPending(pkg)
        // Leaving a gated app doesn't stop its timer immediately — it schedules the stop
        // after LEAVE_GRACE_MS. Only a sustained departure actually cancels the session.
        scheduleLeaveCancels(pkg)
        if (pkg == packageName) return // don't gate our own launcher/gate UI
        maybeGate(pkg)
    }

    /**
     * Schedule a delayed cancel for every active gated app the user just left. Reads the
     * cached active-session set instead of the DB — most app switches happen with no active
     * session, so this stays entirely in memory in the common case.
     */
    private fun scheduleLeaveCancels(currentPkg: String) {
        for (left in activeSessionPackages) {
            if (left == currentPkg) continue
            synchronized(pendingCancels) {
                if (pendingCancels.containsKey(left)) return@synchronized
                pendingCancels[left] = scope.launch {
                    delay(LEAVE_GRACE_MS)
                    // Still not back after the grace? Then it's a real departure — stop it.
                    if (currentForeground != left) {
                        sessionManager.endSession(left)
                    }
                    synchronized(pendingCancels) { pendingCancels.remove(left) }
                }
            }
        }
    }

    private fun cancelPending(pkg: String) {
        synchronized(pendingCancels) { pendingCancels.remove(pkg)?.cancel() }
    }

    private suspend fun maybeGate(pkg: String) {
        // In-memory membership test — the gated set is mirrored from Room by a Flow, so a
        // non-gated app (the overwhelming common case) returns without touching the DB.
        if (pkg !in gatedPackages) return

        val now = System.currentTimeMillis()
        when (sessionManager.gateState(pkg, KICK_COOLDOWN_MS, now)) {
            // Already inside an active session — leave it alone.
            GateState.ACTIVE -> return
            // Just kicked: the app closing emits a foreground event that would otherwise
            // re-prompt instantly. Bounce it home silently instead — this also blocks an
            // immediate un-gated reopen during the cooldown.
            GateState.RECENTLY_KICKED -> {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                return
            }
            GateState.NONE -> Unit
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
