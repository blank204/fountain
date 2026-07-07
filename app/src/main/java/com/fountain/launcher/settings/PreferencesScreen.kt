package com.fountain.launcher.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountain.launcher.ui.theme.FountainPalette

private val IDLE_CHOICES = listOf(0 to "Off", 30 to "30s", 60 to "60s", 120 to "2m")
private val WINDOW_CHOICES = listOf(5 to "5s", 10 to "10s", 20 to "20s")

/** Behavior settings: the DataStore toggles, made reachable (spec §2.3/§2.4/§2.6). */
@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreferencesViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
                text = "Behavior",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono6,
            )
        }

        ToggleRow(
            title = "Breathing screen on reopen",
            subtitle = "After a kick, hold briefly before reopening the same app.",
            checked = settings.reopenBreathingEnabled,
            onCheckedChange = viewModel::setReopenBreathing,
        )
        if (settings.reopenBreathingEnabled) {
            ChoiceRow(
                title = "Reopen window",
                choices = WINDOW_CHOICES,
                selected = settings.reopenWindowSeconds,
                onSelect = viewModel::setReopenWindow,
            )
        }

        ChoiceRow(
            title = "Lock after idle on home",
            choices = IDLE_CHOICES,
            selected = settings.idleTimeoutSeconds,
            onSelect = viewModel::setIdleTimeout,
        )

        ToggleRow(
            title = "CRT overlay",
            subtitle = "Scanlines and flicker on the lock screen.",
            checked = settings.crtOverlayEnabled,
            onCheckedChange = viewModel::setCrtOverlay,
        )

        ToggleRow(
            title = "Require unlock",
            subtitle = "Swiping up asks for your device PIN, pattern, or fingerprint.",
            checked = settings.requireUnlock,
            onCheckedChange = viewModel::setRequireUnlock,
        )

        Text(
            text = "The lock screen is for focus, not security — it doesn't protect your phone.",
            style = MaterialTheme.typography.labelSmall,
            color = FountainPalette.Mono3,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = FountainPalette.Mono6)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = FountainPalette.Mono3,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FountainPalette.MagentaHi,
                checkedTrackColor = FountainPalette.PurpleDeep,
                uncheckedThumbColor = FountainPalette.Mono4,
                uncheckedTrackColor = FountainPalette.Mono1,
            ),
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    choices: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = FountainPalette.Mono6)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            choices.forEach { (value, label) ->
                val isSelected = value == selected
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) FountainPalette.Mono6 else FountainPalette.Mono4,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) FountainPalette.PurpleDeep else FountainPalette.Surface)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
