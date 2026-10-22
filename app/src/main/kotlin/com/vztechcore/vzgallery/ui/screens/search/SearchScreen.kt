package com.vztechcore.vzgallery.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.vztechcore.vzgallery.data.MediaScanner
import com.vztechcore.vzgallery.data.Prefs
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.ui.theme.VzBackground
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary
import com.vztechcore.vzgallery.ui.theme.VzSurface
import com.vztechcore.vzgallery.ui.theme.VzSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SearchAlbum(
    val relativePath: String,
    val displayName: String,
    val matchCount: Int,
    val totalCount: Int,
    val sampleItem: MediaItem
)

@Composable
fun SearchScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scanner = remember { MediaScanner(context) }
    val prefs = remember { Prefs(context) }

    val mostrarSvg by prefs.showSvg.collectAsState(initial = false)
    val albunsFavoritos by prefs.favoriteAlbums.collectAsState(initial = emptySet())

    var query by remember { mutableStateOf("") }
    var allItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var selectedAlbumPath by remember { mutableStateOf<String?>(null) }

    BackHandler {
        if (selectedAlbumPath != null) {
            selectedAlbumPath = null
        } else {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        allItems = withContext(Dispatchers.IO) { scanner.loadMedia() }
        loading = false
    }

    val albumPath = selectedAlbumPath
    if (albumPath != null) {
        SearchAlbumScreen(
            albumPath = albumPath,
            query = query,
            allItems = allItems,
            onBack = { selectedAlbumPath = null }
        )
        return
    }

    val baseItems = remember(allItems, mostrarSvg) {
        if (mostrarSvg) allItems
        else allItems.filterNot {
            it.nome.substringAfterLast('.', "").lowercase() in setOf("svg", "svgz")
        }
    }

    val matchedItems = remember(baseItems, query) {
        if (query.isBlank()) emptyList()
        else baseItems.filter { containsText(it.nome, query) }
    }

    val albums = remember(matchedItems, baseItems) {
        matchedItems
            .groupBy { it.relativePath.ifBlank { "/" } }
            .map { (path, items) ->
                val semBarra = path.trimEnd('/')
                val nome = semBarra.substringAfterLast('/').ifBlank { semBarra.ifBlank { "Raiz" } }
                val totalDoAlbum = baseItems.count {
                    it.relativePath.ifBlank { "/" } == path
                }
                SearchAlbum(
                    relativePath = path,
                    displayName = nome,
                    matchCount = items.size,
                    totalCount = totalDoAlbum,
                    sampleItem = items.first()
                )
            }
            .sortedByDescending { it.matchCount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VzBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = VzOnBackground
                )
            }

            Spacer(Modifier.width(4.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = {
                    Text(
                        "Buscar arquivos...",
                        color = VzOnSurfaceVariant,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = VzSecondary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { query = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Limpar",
                                tint = VzOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VzSecondary,
                    unfocusedBorderColor = VzSurfaceVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = VzSecondary,
                    focusedTextColor = VzOnBackground,
                    unfocusedTextColor = VzOnBackground
                )
            )
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = VzSecondary,
                        trackColor = VzSurfaceVariant
                    )
                }
            }

            query.isBlank() -> {
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
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = VzSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Busque por arquivos",
                        style = MaterialTheme.typography.titleLarge,
                        color = VzOnBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Digite pelo menos uma letra pra ver os resultados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            albums.isEmpty() -> {
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
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = VzSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Nenhum resultado",
                        style = MaterialTheme.typography.titleLarge,
                        color = VzOnBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Nada encontrado com \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VzOnSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "${albums.size} ${if (albums.size == 1) "álbum" else "álbuns"} · ${matchedItems.size} ${if (matchedItems.size == 1) "arquivo" else "arquivos"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VzOnSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    items(albums, key = { it.relativePath }) { album ->
                        SearchAlbumCard(
                            album = album,
                            query = query,
                            isFavorito = album.relativePath in albunsFavoritos,
                            onClick = { selectedAlbumPath = album.relativePath }
                        )
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SearchAlbumCard(
    album: SearchAlbum,
    query: String,
    isFavorito: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VzSurface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VzSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(album.sampleItem.uri)
                    .size(200)
                    .precision(Precision.INEXACT)
                    .crossfade(120)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = VzSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))

                HighlightedText(
                    text = album.displayName,
                    query = query,
                    normalColor = VzOnBackground,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(2.dp))

            HighlightedText(
                text = album.relativePath.trimEnd('/'),
                query = query,
                normalColor = VzOnSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                maxLines = 1
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "${album.totalCount} ${if (album.totalCount == 1) "item" else "itens"}",
                style = MaterialTheme.typography.bodyMedium,
                color = VzSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}