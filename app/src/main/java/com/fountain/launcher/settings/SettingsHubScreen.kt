package com.fountain.launcher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fountain.launcher.ui.theme.FountainPalette

/** Small hub linking the settings surfaces. Rendered in color (not the monochrome home). */
@Composable
fun SettingsHubScreen(
    onGated: () -> Unit,
    onNotifications: () -> Unit,
    onInbox: () -> Unit,
    onBehavior: () -> Unit,
    onHidden: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FountainPalette.Background),
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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono6,
            )
        }

        HubItem("Gated apps", "Apps that ask for a time budget first.", onGated)
        HubItem("Notifications", "Choose which apps land in your inbox.", onNotifications)
        HubItem("Inbox", "Notifications Fountain kept for you.", onInbox)
        HubItem("Behavior", "Breathing screen, lock timing, CRT overlay.", onBehavior)
        HubItem("Hidden apps", "Restore apps you've hidden from the list.", onHidden)
    }
}

@Composable
private fun HubItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.Surface)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = FountainPalette.Mono6)
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = FountainPalette.Mono3,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
