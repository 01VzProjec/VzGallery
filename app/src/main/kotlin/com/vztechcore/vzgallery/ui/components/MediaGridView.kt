package com.vztechcore.vzgallery.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vztechcore.vzgallery.data.GridSize
import com.vztechcore.vzgallery.model.GroupData
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant

private fun nextGridSize(current: GridSize): GridSize? = when (current) {
    GridSize.LARGE -> GridSize.NORMAL
    GridSize.NORMAL -> GridSize.SMALL
    GridSize.SMALL -> GridSize.COMPACT
    GridSize.COMPACT -> null
}

private fun previousGridSize(current: GridSize): GridSize? = when (current) {
    GridSize.COMPACT -> GridSize.SMALL
    GridSize.SMALL -> GridSize.NORMAL
    GridSize.NORMAL -> GridSize.LARGE
    GridSize.LARGE -> null
}

/**
 * Reusable media grid used by HomeScreen, AlbumDetailScreen and SearchAlbumScreen.
 *
 * Handles: grid layout, selection toggle, pinch-to-zoom, fast scrollbar,
 * optional header content, group headers, per-group select-all slot.
 *
 * The caller is responsible for:
 *  - Rendering its own top bar (overlay or Column)
 *  - Rendering the SelectionBottomBar when selectionMode = true
 *  - Managing state (groups, visibleItems, selected)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridView(
    // Data
    groups: List<GroupData>,
    visibleItems: List<MediaItem>,
    loading: Boolean,
    // Selection
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    selectionMode: Boolean,
    allSelected: Boolean,
    // Grid
    gridSize: GridSize,
    onGridSizeChange: (GridSize) -> Unit,
    gridState: LazyGridState,
    // Thumb config
    showName: Boolean,
    showFavoriteBadge: Boolean,
    favorites: Set<String>,
    hiddenItems: Set<String>,
    // Interaction
    onItemClick: (MediaItem, Int) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onPositioned: (uri: String, rect: Rect) -> Unit,
    // Layout
    modifier: Modifier = Modifier,
    contentTopPadding: Dp = 8.dp,
    contentEndPadding: Dp = 44.dp,
    showFastScrollbar: Boolean = true,
    // Slots
    headerContent: (@Composable () -> Unit)? = null,
    sectionHeaderTrailing: (@Composable (GroupData, List<String>) -> Unit)? = null,
    emptyContent: @Composable () -> Unit = { DefaultEmptyState() },
    loadingContent: @Composable () -> Unit = { DefaultLoadingState() }
) {
    val haptic = LocalHapticFeedback.current
    val currentGridSize by rememberUpdatedState(gridSize)
    val currentOnGridSizeChange by rememberUpdatedState(onGridSizeChange)
    val currentHaptic by rememberUpdatedState(haptic)

    val uriToIndex = remember(visibleItems) {
        visibleItems.mapIndexedNotNull { idx, item -> item.uri.toString() to idx }.toMap()
    }

    Box(modifier = modifier.fillMaxSize().background(VzBackground)) {
        when {
            loading -> loadingContent()
            groups.isEmpty() -> emptyContent()
            else -> key(gridSize) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(gridSize.columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var accumulated = 1f
                            awaitEachGesture {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount >= 2) {
                                        val zoom = event.calculateZoom()
                                        if (zoom != 1f && zoom.isFinite() && zoom > 0f) {
                                            accumulated *= zoom
                                            if (accumulated >= 1.25f) {
                                                accumulated = 1f
                                                val prev = previousGridSize(currentGridSize)
                                                if (prev != null) {
                                                    currentHaptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    currentOnGridSizeChange(prev)
                                                }
                                            } else if (accumulated <= 0.8f) {
                                                accumulated = 1f
                                                val next = nextGridSize(currentGridSize)
                                                if (next != null) {
                                                    currentHaptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    currentOnGridSizeChange(next)
                                                }
                                            }
                                            event.changes.forEach {
                                                if (it.pressed) it.consume()
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                                accumulated = 1f
                            }
                        },
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = contentEndPadding,
                        top = contentTopPadding,
                        bottom = if (selectionMode) 110.dp else 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (headerContent != null) {
                        item(
                            key = "header_content",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            Box(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                                headerContent()
                            }
                        }
                    }

                    groups.forEach { group ->
                        item(
                            key = "header_${group.titulo}",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 18.dp, bottom = 8.dp, start = 6.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = group.titulo,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VzOnBackground
                                )
                                if (selectionMode && sectionHeaderTrailing != null) {
                                    val uris = remember(group) {
                                        group.itens.map { it.uri.toString() }
                                    }
                                    sectionHeaderTrailing(group, uris)
                                }
                            }
                        }

                        items(group.itens, key = { it.uri.toString() }) { item ->
                            val uri = item.uri.toString()
                            MediaThumb(
                                item = item,
                                isSelected = uri in selected,
                                isFavorite = uri in favorites,
                                showName = showName,
                                showFavoriteBadge = showFavoriteBadge,
                                showCloudOffBadge = uri in hiddenItems,
                                onClick = {
                                    if (selectionMode) {
                                        onSelectedChange(
                                            if (uri in selected) selected - uri
                                            else selected + uri
                                        )
                                    } else {
                                        val idx = uriToIndex[uri] ?: -1
                                        if (idx >= 0) onItemClick(item, idx)
                                    }
                                },
                                onLongClick = { onItemLongClick(item) },
                                onPositioned = { rect -> onPositioned(uri, rect) }
                            )
                        }
                    }
                }
            }
        }

        if (showFastScrollbar && !loading && groups.isNotEmpty() && !selectionMode) {
            FastScrollbar(
                gridState = gridState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * Helper composable for the "select all in group" button used by HomeScreen.
 * Render this inside the [MediaGridView.sectionHeaderTrailing] slot.
 */
@Composable
fun GroupSelectAllButton(
    groupUris: List<String>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit
) {
    val allSelectedInGroup = groupUris.isNotEmpty() && groupUris.all { it in selected }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (allSelectedInGroup) VzSecondary else VzSurfaceVariant)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onSelectedChange(
                    if (allSelectedInGroup) selected - groupUris.toSet()
                    else selected + groupUris.toSet()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Checklist,
            contentDescription = null,
            tint = if (allSelectedInGroup) VzBackground else VzOnSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DefaultLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = VzSecondary, trackColor = VzSurfaceVariant)
    }
}

@Composable
fun DefaultEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(VzSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = VzSecondary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Nenhuma foto encontrada",
            style = MaterialTheme.typography.titleLarge,
            color = VzOnBackground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Verifique se o app tem permissão para acessar suas fotos.",
            style = MaterialTheme.typography.bodyMedium,
            color = VzOnSurfaceVariant
        )
    }
}