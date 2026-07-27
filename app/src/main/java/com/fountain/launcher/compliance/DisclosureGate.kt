package com.fountain.launcher.compliance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fountain.launcher.ui.theme.FountainPalette

/**
 * Play's prominent-disclosure requirement, rendered. Shown full-screen immediately
 * before Fountain sends the user to a system settings screen to grant a sensitive
 * service — never behind a menu, and never after the fact.
 */
@Composable
fun DisclosureGate(
    service: SensitiveService,
    onConsent: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = disclosureCopyFor(service)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FountainPalette.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = copy.title,
            style = MaterialTheme.typography.titleLarge,
            color = FountainPalette.Mono6,
        )

        DisclosureSection("What Fountain reads", copy.whatIsAccessed)
        DisclosureSection("What it's used for", copy.howItIsUsed)
        DisclosureSection("Where it goes", copy.howItIsShared)

        Button(
            onClick = onConsent,
            colors = ButtonDefaults.buttonColors(
                containerColor = FountainPalette.PurplePrimary,
                contentColor = FountainPalette.Mono6,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(copy.consentLabel, style = MaterialTheme.typography.bodyLarge)
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(copy.dismissLabel, color = FountainPalette.Mono3)
        }
    }
}

@Composable
private fun DisclosureSection(heading: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.Surface)
            .padding(16.dp),
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.MagentaHi,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.labelSmall,
            color = FountainPalette.Mono5,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
