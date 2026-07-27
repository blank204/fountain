package com.fountain.launcher.home

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fountain.launcher.fountain.PixelFountain
import com.fountain.launcher.ui.theme.FountainPalette

/** Saturation-0 filter: renders app icons grayscale on the home surface (spec §2.2). */
private val MonochromeFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Upward drag past this (in px) on the home page opens the app drawer. */
private const val SWIPE_OPEN_THRESHOLD = -140f

/** The app list. Reached by swiping up from the fountain screen; back/↓ returns there. */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    setupIncomplete: Boolean = false,
    onFinishSetup: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Reset search each time the app list opens so it starts clean from the top.
    LaunchedEffect(Unit) { viewModel.onQueryChange("") }
    AppDrawer(
        state = state,
        modifier = modifier,
        setupIncomplete = setupIncomplete,
        onFinishSetup = onFinishSetup,
        onClose = onBack,
        onQueryChange = viewModel::onQueryChange,
        onLaunch = viewModel::onLaunch,
        onOpenAppInfo = viewModel::onOpenAppInfo,
        onHide = viewModel::onHide,
        onSetGated = viewModel::onSetGated,
        onOpenSettings = onOpenSettings,
    )
}

/** The app drawer: search + A–Z list, full screen. Back/↓ returns to the fountain screen. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AppDrawer(
    state: HomeUiState,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onOpenAppInfo: (AppEntry) -> Unit,
    onHide: (AppEntry) -> Unit,
    onSetGated: (AppEntry, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    setupIncomplete: Boolean = false,
    onFinishSetup: () -> Unit = {},
) {
    var menuTarget by remember { mutableStateOf<AppEntry?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FountainPalette.Mono0),
    ) {
        if (setupIncomplete) {
            Text(
                text = "Finish setup →",
                style = MaterialTheme.typography.labelSmall,
                color = FountainPalette.Mono6,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FountainPalette.Mono2)
                    .combinedClickableSimple(onFinishSetup)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "↓",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono4,
                modifier = Modifier.combinedClickableSimple(onClose).padding(end = 8.dp),
            )
            TextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("search", style = MaterialTheme.typography.bodyLarge) },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FountainPalette.Mono1,
                    unfocusedContainerColor = FountainPalette.Mono1,
                    focusedTextColor = FountainPalette.Mono6,
                    unfocusedTextColor = FountainPalette.Mono5,
                    focusedPlaceholderColor = FountainPalette.Mono3,
                    unfocusedPlaceholderColor = FountainPalette.Mono3,
                    cursorColor = FountainPalette.Mono5,
                    focusedIndicatorColor = FountainPalette.Mono2,
                    unfocusedIndicatorColor = FountainPalette.Mono2,
                ),
            )
            Text(
                text = "⚙",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono4,
                modifier = Modifier
                    .combinedClickableSimple(onOpenSettings)
                    .padding(start = 12.dp, end = 4.dp),
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> CenterBox { CircularProgressIndicator(color = FountainPalette.Mono4) }
                state.sections.isEmpty() -> CenterBox {
                    Text(
                        text = if (state.query.isBlank()) "No apps found." else "Nothing matches \"${state.query}\".",
                        style = MaterialTheme.typography.bodyLarge,
                        color = FountainPalette.Mono3,
                    )
                }
                else -> AppList(
                    sections = state.sections,
                    onLaunch = onLaunch,
                    onLongPress = { menuTarget = it },
                )
            }
        }
    }

    menuTarget?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { menuTarget = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = FountainPalette.Surface,
        ) {
            Text(
                text = target.label,
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono6,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = FountainPalette.Mono2)
            val isGated = target.packageName in state.gatedPackages
            SheetAction(if (isGated) "Remove time gate" else "Add time gate") {
                onSetGated(target, !isGated); menuTarget = null
            }
            SheetAction("App info") { onOpenAppInfo(target); menuTarget = null }
            SheetAction("Hide from list") { onHide(target); menuTarget = null }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppList(
    sections: List<Pair<String, List<AppEntry>>>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Snap the list back to the top each time home is returned to, rather than restoring
    // the previous scroll position.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { listState.scrollToItem(0) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Flattened index of each section header, for the fast-scroll rail. Each section
    // contributes 1 header + its apps.
    val headerIndex = remember(sections) {
        buildMap {
            var index = 0
            sections.forEach { (letter, apps) ->
                put(letter, index)
                index += 1 + apps.size
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            sections.forEach { (letter, apps) ->
                stickyHeader(key = "hdr_$letter") { SectionHeader(letter) }
                items(apps, key = { it.packageName + it.component.className }) { app ->
                    AppRow(
                        app = app,
                        onClick = { onLaunch(app) },
                        onLongClick = { onLongPress(app) },
                    )
                }
            }
        }

        if (sections.size > 1) {
            val letters = remember(sections) { sections.map { it.first } }
            var railHeightPx by remember { mutableIntStateOf(1) }

            fun scrubTo(y: Float) {
                val fraction = (y / railHeightPx.toFloat()).coerceIn(0f, 1f)
                val index = (fraction * letters.size).toInt().coerceIn(0, letters.lastIndex)
                headerIndex[letters[index]]?.let { target ->
                    scope.launch { listState.scrollToItem(target) }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .onSizeChanged { railHeightPx = it.height.coerceAtLeast(1) }
                    .pointerInput(letters) {
                        detectVerticalDragGestures(
                            onDragStart = { offset -> scrubTo(offset.y) },
                            onVerticalDrag = { change, _ -> scrubTo(change.position.y) },
                        )
                    }
                    .pointerInput(letters) {
                        detectTapGestures { offset -> scrubTo(offset.y) }
                    }
                    .padding(horizontal = 8.dp),
            ) {
                letters.forEach { letter ->
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelSmall,
                        color = FountainPalette.Mono4,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAppButton(quick: QuickApp, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        // Pixel-ify: downscale hard, then upscale with no smoothing for chunky pixel edges.
        val image = remember(quick.icon) {
            val full = quick.icon.toBitmap(width = 96, height = 96)
            Bitmap.createScaledBitmap(full, 22, 22, false).asImageBitmap()
        }
        Image(
            bitmap = image,
            contentDescription = quick.label,
            colorFilter = MonochromeFilter,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Text(
            text = quick.label,
            style = MaterialTheme.typography.labelSmall,
            color = FountainPalette.Mono4,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SectionHeader(letter: String) {
    Text(
        text = letter,
        style = MaterialTheme.typography.labelSmall,
        color = FountainPalette.Mono4,
        modifier = Modifier
            .fillMaxWidth()
            .background(FountainPalette.Mono0)
            .padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        AppIcon(app.icon)
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.Mono5,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppIcon(drawable: Drawable) {
    val image = remember(drawable) { drawable.toBitmap(width = 144, height = 144).asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = null,
        colorFilter = MonochromeFilter,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = FountainPalette.Mono5,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickableSimple(onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableSimple(onClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick)

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
