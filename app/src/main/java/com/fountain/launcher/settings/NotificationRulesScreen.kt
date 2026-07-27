package com.fountain.launcher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountain.launcher.common.NotificationAccessUtil
import com.fountain.launcher.data.NotificationMode
import com.fountain.launcher.home.AppEntry
import com.fountain.launcher.ui.theme.FountainPalette

private data class ModeChoice(val mode: NotificationMode, val label: String)

private val CHOICES = listOf(
    ModeChoice(NotificationMode.PASSTHROUGH, "Leave"),
    ModeChoice(NotificationMode.SHOW, "Inbox"),
    ModeChoice(NotificationMode.SUPPRESS, "Mute"),
)

@Composable
fun NotificationRulesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationRulesViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            Column {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    color = FountainPalette.Mono6,
                )
                Text(
                    text = "Leave keeps them native. Inbox captures them here. Mute removes them.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FountainPalette.Mono3,
                )
            }
        }

        if (!rememberNotificationAccessEnabled()) {
            AccessBanner()
        }

        TextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            singleLine = true,
            placeholder = { Text("search", style = MaterialTheme.typography.bodyLarge) },
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FountainPalette.Surface,
                unfocusedContainerColor = FountainPalette.Surface,
                focusedTextColor = FountainPalette.Mono6,
                unfocusedTextColor = FountainPalette.Mono5,
                focusedPlaceholderColor = FountainPalette.Mono3,
                unfocusedPlaceholderColor = FountainPalette.Mono3,
                cursorColor = FountainPalette.PurplePrimary,
                focusedIndicatorColor = FountainPalette.PurpleDeep,
                unfocusedIndicatorColor = FountainPalette.Mono2,
            ),
        )

        when {
            state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = FountainPalette.PurplePrimary)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.apps, key = { it.packageName + it.component.className }) { app ->
                    RuleRow(
                        app = app,
                        mode = state.rules[app.packageName] ?: NotificationMode.PASSTHROUGH,
                        onSetMode = { viewModel.onSetMode(app, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleRow(app: AppEntry, mode: NotificationMode, onSetMode: (NotificationMode) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.Mono5,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            CHOICES.forEach { choice ->
                ModeChip(
                    label = choice.label,
                    selected = mode == choice.mode,
                    onClick = { onSetMode(choice.mode) },
                )
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) FountainPalette.Mono6 else FountainPalette.Mono4,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FountainPalette.PurpleDeep else FountainPalette.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun AccessBanner() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.PurpleDeep)
            .clickable { NotificationAccessUtil.openSettings(context) }
            .padding(16.dp),
    ) {
        Text(
            text = "Notification access is off",
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.Mono6,
        )
        Text(
            text = "Inbox and Mute need it. Leave still works without it. Tap to enable.",
            style = MaterialTheme.typography.labelSmall,
            color = FountainPalette.Mono5,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun rememberNotificationAccessEnabled(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(NotificationAccessUtil.isEnabled(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = NotificationAccessUtil.isEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}
