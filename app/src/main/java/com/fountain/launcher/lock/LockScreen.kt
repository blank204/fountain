package com.fountain.launcher.lock

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountain.launcher.R
import com.fountain.launcher.common.DeviceLock
import com.fountain.launcher.common.MotionUtil
import com.fountain.launcher.data.CapturedNotificationEntity
import com.fountain.launcher.fountain.CrtOverlay
import com.fountain.launcher.fountain.PixelFountain
import com.fountain.launcher.home.QuickApp
import com.fountain.launcher.ui.theme.FountainPalette
import com.fountain.launcher.ui.theme.PixelFont
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val UNLOCK_THRESHOLD_PX = -160f

/**
 * Full-screen focus lock on the home surface (spec §2.4). In color: animated fountain,
 * pixel clock + date, and the top captured notifications. Swipe up to dismiss. Cosmetic,
 * not security — documented as such.
 */
@Composable
fun LockScreen(
    crtEnabled: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LockViewModel = viewModel(),
) {
    val notifications by viewModel.topNotifications.collectAsStateWithLifecycle()
    val quickApps by viewModel.quickApps.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val reducedMotion = remember { MotionUtil.isReducedMotion(context) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FountainPalette.Background)
            .pointerInput(Unit) {
                // Double-tap the fountain screen to turn the phone off (device-admin lock).
                detectTapGestures(onDoubleTap = { DeviceLock.lockNow(context) })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset <= UNLOCK_THRESHOLD_PX) onUnlock()
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, delta -> dragOffset += delta },
                )
            },
    ) {
        if (reducedMotion) {
            // Static motif instead of the animated fountain when animations are off.
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(160.dp),
            )
        } else {
            PixelFountain(modifier = Modifier.fillMaxSize(), running = true)
            CrtOverlay(modifier = Modifier.fillMaxSize(), enabled = crtEnabled)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 96.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = clockFormat.format(Date(now)),
                style = TextStyle(fontFamily = PixelFont, fontSize = 72.sp),
                color = FountainPalette.Mono6,
            )
            Text(
                text = dateFormat.format(Date(now)),
                style = TextStyle(fontFamily = PixelFont, fontSize = 20.sp),
                color = FountainPalette.MagentaHi,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            notifications.forEach { LockNotification(it) }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                quickApps.forEach { quick ->
                    LockQuickApp(quick = quick, onClick = { viewModel.onQuickLaunch(quick) })
                }
            }
            Text(
                text = "swipe up for apps",
                style = TextStyle(fontFamily = PixelFont, fontSize = 14.sp),
                color = FountainPalette.Mono4,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun LockQuickApp(quick: QuickApp, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        val image = remember(quick.icon) {
            val full = quick.icon.toBitmap(width = 96, height = 96)
            Bitmap.createScaledBitmap(full, 24, 24, false).asImageBitmap()
        }
        Image(
            bitmap = image,
            contentDescription = quick.label,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Text(
            text = quick.label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = FountainPalette.Mono4,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun LockNotification(item: CapturedNotificationEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FountainPalette.Surface)
            .padding(14.dp),
    ) {
        Text(
            text = item.appLabel,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = FountainPalette.PurplePrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.title.isNotBlank()) {
            Text(
                text = item.title,
                style = TextStyle(fontFamily = PixelFont, fontSize = 16.sp),
                color = FountainPalette.Mono6,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
