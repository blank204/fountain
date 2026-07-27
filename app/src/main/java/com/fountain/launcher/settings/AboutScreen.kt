package com.fountain.launcher.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fountain.launcher.BuildConfig
import com.fountain.launcher.data.FountainSettings
import com.fountain.launcher.data.SettingsRepository
import com.fountain.launcher.ui.theme.FountainPalette

private const val POLICY_URL = "https://blank204.github.io/fountain/privacy"

/**
 * Privacy policy, third-party licences, and consent status.
 *
 * The policy is read from a bundled asset rather than fetched, so it works with no
 * network — Fountain has no internet permission and should not need one to show its
 * own policy. PrivacyPolicyMirrorTest keeps that asset identical to the published one.
 */
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by remember { SettingsRepository(context).settings }
        .collectAsStateWithLifecycle(initialValue = FountainSettings())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FountainPalette.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = "←",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono5,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 4.dp),
            )
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono6,
            )
        }

        Card {
            Text(
                "Fountain ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                color = FountainPalette.Mono6,
            )
            Body("Fountain has no internet permission. It collects nothing and transmits nothing.")
        }

        Card {
            Heading("Permissions you've acknowledged")
            Body(
                "Accessibility disclosure: " +
                    if (settings.consentAccessibility) "acknowledged" else "not yet shown"
            )
            Body(
                "Notification disclosure: " +
                    if (settings.consentNotifications) "acknowledged" else "not yet shown"
            )
        }

        ExpandableCard("Privacy policy", assetName = "legal/privacy-policy.md")

        Card {
            Heading("Open the policy online")
            Body(POLICY_URL)
            Text(
                "Open in browser",
                style = MaterialTheme.typography.labelSmall,
                color = FountainPalette.MagentaHi,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(POLICY_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
            )
        }

        ExpandableCard("Fountain — MIT License", assetName = "licenses/MIT.txt")
        ExpandableCard("Pixelify Sans — SIL Open Font License 1.1", assetName = "licenses/OFL-1.1.txt")
        ExpandableCard(
            "AndroidX, Compose, Room, Kotlin — Apache License 2.0",
            assetName = "licenses/Apache-2.0.txt",
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.Surface)
            .padding(20.dp),
    ) { content() }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = FountainPalette.Mono6)
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = FountainPalette.Mono3,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Collapsed by default — these texts are long and would bury everything else. */
@Composable
private fun ExpandableCard(title: String, assetName: String) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    // A missing licence asset must never crash the app.
    val text = remember(assetName, expanded) {
        if (!expanded) "" else runCatching {
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        }.getOrElse { "Could not load $assetName." }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.Surface)
            .clickable { expanded = !expanded }
            .padding(20.dp),
    ) {
        Text(
            text = (if (expanded) "▾ " else "▸ ") + title,
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.Mono6,
        )
        if (expanded) Body(text)
    }
}
