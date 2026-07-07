package com.fountain.launcher.settings

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountain.launcher.common.AccessibilityUtil
import com.fountain.launcher.home.AppEntry
import com.fountain.launcher.ui.theme.FountainPalette

/**
 * Settings surface, so it renders in color (not the monochrome home). A switch per app
 * toggles the time gate; changes persist immediately via the view model.
 */
@Composable
fun GatedAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GatedAppsViewModel = viewModel(),
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
                    text = "Gated apps",
                    style = MaterialTheme.typography.titleLarge,
                    color = FountainPalette.Mono6,
                )
                Text(
                    text = "Opening these asks for a time budget first.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FountainPalette.Mono3,
                )
            }
        }

        if (!rememberAccessibilityEnabled()) {
            ForceKickBanner()
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
                    GatedRow(
                        app = app,
                        gated = app.packageName in state.gatedPackages,
                        onToggle = { checked -> viewModel.onToggle(app, checked) },
                    )
                }
            }
        }
    }
}

/** Re-reads the accessibility grant each time the screen resumes (e.g. back from Settings). */
@Composable
private fun rememberAccessibilityEnabled(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(AccessibilityUtil.isServiceEnabled(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = AccessibilityUtil.isServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

@Composable
private fun ForceKickBanner() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.PurpleDeep)
            .clickable { AccessibilityUtil.openSettings(context) }
            .padding(16.dp),
    ) {
        Text(
            text = "Force-kick is off",
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.Mono6,
        )
        Text(
            text = "Without accessibility, Fountain can ask for time but can't send you home when it's up. Tap to enable.",
            style = MaterialTheme.typography.labelSmall,
            color = FountainPalette.Mono5,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun GatedRow(app: AppEntry, gated: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!gated) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        ColorAppIcon(app.icon)
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.Mono5,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = gated,
            onCheckedChange = onToggle,
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
private fun ColorAppIcon(drawable: Drawable) {
    val image = androidx.compose.runtime.remember(drawable) {
        drawable.toBitmap(width = 144, height = 144).asImageBitmap()
    }
    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp)),
    )
}
