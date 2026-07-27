package com.fountain.launcher.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fountain.launcher.common.AccessibilityUtil
import com.fountain.launcher.common.BiometricAuth
import com.fountain.launcher.common.NotificationAccessUtil
import com.fountain.launcher.common.SystemAccess
import com.fountain.launcher.data.FountainSettings
import com.fountain.launcher.data.SettingsRepository
import com.fountain.launcher.home.HomeScreen
import com.fountain.launcher.lock.LockScreen
import com.fountain.launcher.notifications.InboxScreen
import com.fountain.launcher.onboarding.OnboardingScreen
import com.fountain.launcher.settings.AboutScreen
import com.fountain.launcher.settings.GatedAppsScreen
import com.fountain.launcher.settings.HiddenAppsScreen
import com.fountain.launcher.settings.NotificationRulesScreen
import com.fountain.launcher.settings.PreferencesScreen
import com.fountain.launcher.settings.SettingsHubScreen
import com.fountain.launcher.ui.theme.FountainPalette
import com.fountain.launcher.ui.theme.FountainTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Screens reachable from the launcher host. Kept as a tiny enum — no nav library needed yet. */
private enum class Screen {
    Home, Onboarding, Settings, GatedApps, NotificationRules, Inbox, Behavior, HiddenApps, About
}

// FragmentActivity (not ComponentActivity) so androidx.biometric can host its prompt.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FountainTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FountainPalette.Mono0,
                ) {
                    FountainApp()
                }
            }
        }
    }
}

@Composable
private fun FountainApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsFlow = remember { SettingsRepository(context).settings }
    // initialValue marks onboarding "seen" so the first-run screen never flashes before
    // the real value loads; the real value drives the effect below.
    val settings by settingsFlow.collectAsStateWithLifecycle(
        initialValue = FountainSettings(onboardingSeen = true)
    )

    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    // Lock trigger (spec §2.4): starts locked, and re-locks on every return to the home
    // surface (each time the activity resumes) — except while onboarding.
    var locked by rememberSaveable { mutableStateOf(true) }
    var permCheck by remember { mutableIntStateOf(0) }
    // Suppresses exactly one re-lock: set before launching the unlock prompt so returning
    // from the system credential screen doesn't immediately re-lock a successful unlock.
    var suppressNextRelock by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permCheck++
                if (screen != Screen.Onboarding && !suppressNextRelock) locked = true
                suppressNextRelock = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // First run: drop into onboarding once, unlocked.
    var onboardingRouted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.onboardingSeen) {
        if (!settings.onboardingSeen && !onboardingRouted) {
            onboardingRouted = true
            screen = Screen.Onboarding
            locked = false
        }
    }

    val setupIncomplete = remember(permCheck) {
        !SystemAccess.isDefaultLauncher(context) ||
            !AccessibilityUtil.isServiceEnabled(context) ||
            !NotificationAccessUtil.isEnabled(context)
    }

    // Optional idle-while-on-home timeout: re-lock after N seconds unlocked.
    LaunchedEffect(locked, settings.idleTimeoutSeconds) {
        if (!locked && settings.idleTimeoutSeconds > 0) {
            delay(settings.idleTimeoutSeconds * 1_000L)
            locked = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Content is inset from the status bar and nav buttons; the lock overlay below is
        // intentionally full-bleed so the fountain runs edge to edge. Skip composing the
        // home entirely while locked so its fountain doesn't animate behind the overlay.
        if (!locked) Box(Modifier.fillMaxSize().systemBarsPadding()) {
        when (screen) {
            Screen.Home -> {
                // Back from the app list returns to the fountain screen (locks), rather
                // than exiting the launcher.
                BackHandler { locked = true }
                HomeScreen(
                    onOpenSettings = { screen = Screen.Settings },
                    setupIncomplete = setupIncomplete,
                    onFinishSetup = { screen = Screen.Onboarding },
                    onBack = { locked = true },
                )
            }

            Screen.Onboarding -> {
                BackHandler { screen = Screen.Home }
                OnboardingScreen(
                    onFinish = {
                        scope.launch { SettingsRepository(context).setOnboardingSeen(true) }
                        screen = Screen.Home
                    }
                )
            }

            Screen.Settings -> {
                BackHandler { screen = Screen.Home }
                SettingsHubScreen(
                    onGated = { screen = Screen.GatedApps },
                    onNotifications = { screen = Screen.NotificationRules },
                    onInbox = { screen = Screen.Inbox },
                    onBehavior = { screen = Screen.Behavior },
                    onHidden = { screen = Screen.HiddenApps },
                    onAbout = { screen = Screen.About },
                    onBack = { screen = Screen.Home },
                )
            }

            Screen.GatedApps -> {
                BackHandler { screen = Screen.Settings }
                GatedAppsScreen(onBack = { screen = Screen.Settings })
            }

            Screen.NotificationRules -> {
                BackHandler { screen = Screen.Settings }
                NotificationRulesScreen(onBack = { screen = Screen.Settings })
            }

            Screen.Inbox -> {
                BackHandler { screen = Screen.Settings }
                InboxScreen(onBack = { screen = Screen.Settings })
            }

            Screen.Behavior -> {
                BackHandler { screen = Screen.Settings }
                PreferencesScreen(onBack = { screen = Screen.Settings })
            }

            Screen.HiddenApps -> {
                BackHandler { screen = Screen.Settings }
                HiddenAppsScreen(onBack = { screen = Screen.Settings })
            }

            Screen.About -> {
                BackHandler { screen = Screen.Settings }
                AboutScreen(onBack = { screen = Screen.Settings })
            }
        }
        }

        if (locked) {
            // Swallow back while locked so it can't dismiss the overlay.
            BackHandler(enabled = true) {}
            val activity = remember(context) { BiometricAuth.findActivity(context) }
            LockScreen(
                crtEnabled = settings.crtOverlayEnabled,
                onUnlock = {
                    // With "require unlock" on, confirm the device credential first;
                    // otherwise a swipe is enough. Fall back to swipe if no credential exists.
                    if (settings.requireUnlock && activity != null &&
                        BiometricAuth.canAuthenticate(context)
                    ) {
                        suppressNextRelock = true
                        BiometricAuth.authenticate(
                            activity = activity,
                            onSuccess = { locked = false },
                            onFailure = { /* stay locked */ },
                        )
                    } else {
                        locked = false
                    }
                },
            )
        }
    }
}
