package com.fountain.launcher.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fountain.launcher.ui.theme.FountainPalette

/** UI states for the gate flow. */
sealed interface GateUi {
    data object Loading : GateUi
    data class Breathing(val remaining: Int) : GateUi
    data object Presets : GateUi
}

/** Holds the observable gate state the activity mutates from its decision coroutine. */
class GateUiController {
    val state: MutableState<GateUi> = mutableStateOf(GateUi.Loading)
}

private val PRESETS = listOf(1, 5, 10)

@Composable
fun GateScreen(
    appLabel: String,
    state: GateUi,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FountainPalette.Background)
            // Insets first, then the design padding: from targetSdk 35 edge-to-edge is
            // enforced with no opt-out, so without this the presets sit under the status
            // and navigation bars.
            .systemBarsPadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            GateUi.Loading -> CircularProgressIndicator(color = FountainPalette.PurplePrimary)

            is GateUi.Breathing -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Take a breath.",
                    style = MaterialTheme.typography.titleLarge,
                    color = FountainPalette.Mono6,
                )
                Text(
                    text = "$appLabel opens in ${state.remaining}…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FountainPalette.Mono3,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            GateUi.Presets -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "How long in $appLabel?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = FountainPalette.Mono6,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "When the time is up, you're back home.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FountainPalette.Mono3,
                    textAlign = TextAlign.Center,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    PRESETS.forEach { PresetButton(it, onPick) }
                }
                TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Not now", color = FountainPalette.Mono4)
                }
            }
        }
    }
}

@Composable
private fun PresetButton(minutes: Int, onPick: (Int) -> Unit) {
    Button(
        onClick = { onPick(minutes) },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FountainPalette.Surface,
            contentColor = FountainPalette.PurplePrimary,
        ),
        modifier = Modifier.size(width = 88.dp, height = 66.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$minutes",
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = FountainPalette.PurplePrimary,
            )
            Text("min", style = MaterialTheme.typography.labelSmall, color = FountainPalette.Mono4)
        }
    }
}
