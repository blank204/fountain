package com.fountain.launcher.gate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.fountain.launcher.data.SettingsRepository
import com.fountain.launcher.ui.theme.FountainTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The time-request surface (spec §2.3). Launched when a gated app is opened. Decides the
 * flow before showing anything:
 *  - an active, unexpired session → open the app immediately (reuse remaining time),
 *  - a recent kick with breathing enabled → a short hold, then presets,
 *  - otherwise → presets straight away.
 */
class GateActivity : ComponentActivity() {

    private lateinit var targetPackage: String
    private var targetComponent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Matches MainActivity. From targetSdk 35 edge-to-edge is enforced anyway; calling
        // it explicitly keeps the inset behaviour the same on older releases too.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        targetComponent = intent.getStringExtra(EXTRA_COMPONENT)

        val ui = GateUiController()
        setContent {
            FountainTheme {
                GateScreen(
                    appLabel = intent.getStringExtra(EXTRA_LABEL) ?: targetPackage,
                    state = ui.state.value,
                    onPick = { minutes -> confirm(minutes) },
                    onCancel = { goHomeAndFinish() },
                )
            }
        }

        lifecycleScope.launch { decideFlow(ui) }
    }

    private suspend fun decideFlow(ui: GateUiController) {
        val now = System.currentTimeMillis()
        val manager = SessionManager(applicationContext)

        if (manager.activeSession(targetPackage, now) != null) {
            launchTarget(); finish(); return
        }

        val settings = SettingsRepository(applicationContext).settings.first()
        val windowMs = settings.reopenWindowSeconds * 1000L
        if (settings.reopenBreathingEnabled && manager.kickedWithin(targetPackage, windowMs, now)) {
            for (s in HOLD_SECONDS downTo 1) {
                ui.state.value = GateUi.Breathing(s)
                delay(1_000)
            }
        }
        ui.state.value = GateUi.Presets
    }

    private fun confirm(minutes: Int) {
        lifecycleScope.launch {
            SessionManager(applicationContext).startSession(
                packageName = targetPackage,
                durationMs = minutes * 60_000L,
                now = System.currentTimeMillis(),
            )
            launchTarget()
            finish()
        }
    }

    private fun launchTarget() {
        val component = targetComponent?.let(ComponentName::unflattenFromString)
        if (component != null) {
            val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
            return
        }
        // Caught over an already-running app: bring it back to the front explicitly.
        // Finishing this activity returns to our own task (home), not the app's task, so
        // without this the app appears to "minimise" the instant you confirm.
        packageManager.getLaunchIntentForPackage(targetPackage)?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        }
    }

    private fun goHomeAndFinish() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_COMPONENT = "component"
        const val EXTRA_LABEL = "label"
        private const val HOLD_SECONDS = 5

        /** Launched from the home list: we know the component to open on confirm. */
        fun open(context: Context, packageName: String, component: ComponentName, label: String) {
            context.startActivity(intentFor(context, packageName, component.flattenToString(), label))
        }

        /**
         * Launched by the AccessibilityService when a gated app is already foreground:
         * no component to open — confirming just dismisses the gate over the running app.
         */
        fun openForForeground(context: Context, packageName: String, label: String) {
            context.startActivity(intentFor(context, packageName, component = null, label = label))
        }

        private fun intentFor(
            context: Context,
            packageName: String,
            component: String?,
            label: String,
        ): Intent = Intent(context, GateActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_PACKAGE, packageName)
            component?.let { putExtra(EXTRA_COMPONENT, it) }
            putExtra(EXTRA_LABEL, label)
        }
    }
}
