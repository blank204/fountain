package com.fountain.launcher.notifications

import android.text.format.DateUtils
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountain.launcher.R
import com.fountain.launcher.data.CapturedNotificationEntity
import com.fountain.launcher.ui.theme.FountainPalette
import com.fountain.launcher.ui.theme.PixelFont

/**
 * The pixel inbox (spec §2.5). Renders in color with the pixel font and a single static
 * fountain motif in the header — the animated fountain is reserved for the lock overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = viewModel(),
) {
    val inbox by viewModel.inbox.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<CapturedNotificationEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FountainPalette.Background),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "←",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono5,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp),
            )
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = "INBOX",
                style = TextStyle(fontFamily = PixelFont, fontSize = 24.sp),
                color = FountainPalette.MagentaHi,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            if (inbox.isNotEmpty()) {
                Text(
                    text = "clear",
                    style = MaterialTheme.typography.labelSmall,
                    color = FountainPalette.Mono4,
                    modifier = Modifier.clickable(onClick = viewModel::onClear).padding(8.dp),
                )
            }
        }
        HorizontalDivider(color = FountainPalette.Mono2)

        if (inbox.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    text = "Nothing here yet.\nApps set to Inbox will show up here.",
                    style = TextStyle(fontFamily = PixelFont, fontSize = 16.sp),
                    color = FountainPalette.Mono3,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(inbox, key = { it.id }) { item ->
                    InboxRow(item, onClick = { detail = item })
                }
            }
        }
    }

    detail?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { detail = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = FountainPalette.Surface,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    text = item.appLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = FountainPalette.PurplePrimary,
                )
                if (item.title.isNotBlank()) {
                    Text(
                        text = item.title,
                        style = TextStyle(fontFamily = PixelFont, fontSize = 22.sp),
                        color = FountainPalette.Mono6,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (item.text.isNotBlank()) {
                    Text(
                        text = item.text,
                        style = TextStyle(fontFamily = PixelFont, fontSize = 16.sp),
                        color = FountainPalette.Mono4,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxRow(item: CapturedNotificationEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.appLabel,
                style = MaterialTheme.typography.labelSmall,
                color = FountainPalette.PurplePrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = DateUtils.getRelativeTimeSpanString(item.postedAtEpochMs).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = FountainPalette.Mono3,
            )
        }
        if (item.title.isNotBlank()) {
            Text(
                text = item.title,
                style = TextStyle(fontFamily = PixelFont, fontSize = 18.sp),
                color = FountainPalette.Mono6,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (item.text.isNotBlank()) {
            Text(
                text = item.text,
                style = TextStyle(fontFamily = PixelFont, fontSize = 14.sp),
                color = FountainPalette.Mono4,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
