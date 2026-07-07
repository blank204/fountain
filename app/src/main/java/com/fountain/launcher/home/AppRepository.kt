package com.fountain.launcher.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings

/**
 * Reads and launches installed apps via [LauncherApps] (the launcher-blessed API, which
 * sees launchable activities without extra package-visibility work).
 *
 * Hidden apps are persisted in a lightweight SharedPreferences set for M2. When the Room/
 * DataStore layer lands (M3), gated-app state moves there; hidden can migrate alongside it.
 */
class AppRepository(private val context: Context) {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val prefs = context.getSharedPreferences("fountain_home", Context.MODE_PRIVATE)

    private val ownPackage = context.packageName

    fun hiddenPackages(): Set<String> =
        prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty()

    fun setHidden(packageName: String, hidden: Boolean) {
        val current = hiddenPackages().toMutableSet()
        if (hidden) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN, current).apply()
    }

    /**
     * Launchable apps except our own launcher, sorted A–Z. The home surface hides
     * user-hidden apps; the gated-app picker passes [includeHidden] = true so any app
     * can still be gated.
     */
    fun loadApps(includeHidden: Boolean = false): List<AppEntry> {
        val user = Process.myUserHandle()
        val hidden = if (includeHidden) emptySet() else hiddenPackages()
        return launcherApps.getActivityList(null, user)
            .asSequence()
            .filter { it.applicationInfo.packageName != ownPackage }
            .filter { it.applicationInfo.packageName !in hidden }
            .map {
                AppEntry(
                    packageName = it.applicationInfo.packageName,
                    label = it.label.toString(),
                    icon = it.getIcon(0),
                    component = it.componentName,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** The apps the user has hidden, for the "hidden apps" management screen. */
    fun loadHiddenApps(): List<AppEntry> {
        val hidden = hiddenPackages()
        if (hidden.isEmpty()) return emptyList()
        val user = Process.myUserHandle()
        return launcherApps.getActivityList(null, user)
            .asSequence()
            .filter { it.applicationInfo.packageName in hidden }
            .map {
                AppEntry(
                    packageName = it.applicationInfo.packageName,
                    label = it.label.toString(),
                    icon = it.getIcon(0),
                    component = it.componentName,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launch(component: ComponentName) {
        launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
    }

    /** The two pinned home shortcuts: Phone and Camera, resolved to the user's defaults. */
    fun loadQuickApps(): List<QuickApp> {
        val pm = context.packageManager
        val specs = listOf(
            "Phone" to Intent(Intent.ACTION_DIAL),
            "Camera" to Intent("android.media.action.STILL_IMAGE_CAMERA"),
        )
        return specs.mapNotNull { (fallback, intent) ->
            val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?: return@mapNotNull null
            QuickApp(
                label = resolved.loadLabel(pm)?.toString()?.ifBlank { fallback } ?: fallback,
                icon = resolved.loadIcon(pm),
                intent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun launchIntent(intent: Intent) {
        context.startActivity(intent)
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private companion object {
        const val KEY_HIDDEN = "hidden_packages"
    }
}
