package com.fountain.launcher.onboarding

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fountain.launcher.common.AccessibilityUtil
import com.fountain.launcher.common.DeviceLock
import com.fountain.launcher.common.NotificationAccessUtil
import com.fountain.launcher.common.SystemAccess
import com.fountain.launcher.ui.theme.FountainPalette

/**
 * First-run setup (spec §4). Steps are ordered by importance — default launcher and the
 * Samsung battery step first, since without them force-kick silently fails. Statuses
 * refresh whenever the screen resumes, so it's resumable rather than a one-shot wizard.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var refresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    // Reading `refresh` here makes every status below recompute on resume.
    @Suppress("UNUSED_EXPRESSION") refresh

    val postNotifGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FountainPalette.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Set up Fountain",
            style = MaterialTheme.typography.titleLarge,
            color = FountainPalette.Mono6,
        )
        Text(
            text = "Grant these in order. You can revoke any of them later, and skip the optional ones.",
            style = MaterialTheme.typography.labelSmall,
            color = FountainPalette.Mono3,
        )

        Step(
            title = "Make Fountain your home",
            why = "So it takes over as your launcher.",
            done = SystemAccess.isDefaultLauncher(context),
            action = "Choose",
            onAction = { SystemAccess.openHomeSettings(context) },
        )
        Step(
            title = "Keep Fountain awake",
            why = "Samsung sleeps background apps. Without this, force-kick and notifications stop when the phone idles. Add Fountain to “never sleeping apps” too.",
            done = SystemAccess.isIgnoringBatteryOptimizations(context),
            action = "Allow",
            onAction = { SystemAccess.requestIgnoreBatteryOptimizations(context) },
        )
        Step(
            title = "Enable force-kick",
            why = "Accessibility lets Fountain notice a gated app and send you home when time is up.",
            done = AccessibilityUtil.isServiceEnabled(context),
            action = "Enable",
            onAction = { AccessibilityUtil.openSettings(context) },
        )
        Step(
            title = "Show the gate over apps",
            why = "Lets the time picker pop up on top of a gated app — not only inside Fountain.",
            done = SystemAccess.canDrawOverlays(context),
            action = "Allow",
            onAction = { SystemAccess.openOverlaySettings(context) },
        )
        Step(
            title = "Capture notifications",
            why = "Optional. Lets the inbox and mute rules work.",
            done = NotificationAccessUtil.isEnabled(context),
            action = "Grant",
            onAction = { NotificationAccessUtil.openSettings(context) },
        )
        Step(
            title = "Let Fountain post notifications",
            why = "Optional. Shows the session countdown and inbox alerts.",
            done = postNotifGranted,
            action = "Allow",
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
        Step(
            title = "Precise timing",
            why = "Recommended. Fires the kick on time even in Doze.",
            done = SystemAccess.canScheduleExactAlarms(context),
            action = "Allow",
            onAction = { SystemAccess.openExactAlarmSettings(context) },
        )
        Step(
            title = "Double-tap to lock",
            why = "Optional. Lets a double-tap on the fountain turn the screen off.",
            done = DeviceLock.isAdminActive(context),
            action = "Enable",
            onAction = { DeviceLock.requestAdmin(context) },
        )

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(
                containerColor = FountainPalette.PurplePrimary,
                contentColor = FountainPalette.Mono6,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Done", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Step(
    title: String,
    why: String,
    done: Boolean,
    action: String,
    onAction: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.Surface)
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = (if (done) "✓ " else "") + title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) FountainPalette.CyanGlow else FountainPalette.Mono6,
            )
            Text(
                text = why,
                style = MaterialTheme.typography.labelSmall,
                color = FountainPalette.Mono3,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (!done) {
            TextButton(onClick = onAction) {
                Text(action, color = FountainPalette.MagentaHi)
            }
        }
    }
}
