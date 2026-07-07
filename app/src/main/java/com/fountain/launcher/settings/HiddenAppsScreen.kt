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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountain.launcher.home.AppEntry
import com.fountain.launcher.ui.theme.FountainPalette

@Composable
fun HiddenAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HiddenAppsViewModel = viewModel(),
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
            Text(
                text = "Hidden apps",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono6,
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = FountainPalette.PurplePrimary)
            }
            state.apps.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    text = "No hidden apps.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FountainPalette.Mono3,
                )
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.apps, key = { it.packageName + it.component.className }) { app ->
                    HiddenRow(app = app, onUnhide = { viewModel.onUnhide(app) })
                }
            }
        }
    }
}

@Composable
private fun HiddenRow(app: AppEntry, onUnhide: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        AppIcon(app.icon)
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.Mono5,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUnhide) {
            Text("Unhide", color = FountainPalette.MagentaHi)
        }
    }
}

@Composable
private fun AppIcon(drawable: Drawable) {
    val image = remember(drawable) { drawable.toBitmap(width = 144, height = 144).asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp)),
    )
}
