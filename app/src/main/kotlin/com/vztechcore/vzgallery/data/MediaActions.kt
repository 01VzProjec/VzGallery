package com.vztechcore.vzgallery.data

import android.content.Context
import android.content.Intent
import com.vztechcore.vzgallery.model.MediaItem
import com.vztechcore.vzgallery.util.VzLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * Shared file operations used by HomeScreen, AlbumDetailScreen and SearchAlbumScreen.
 * Each function returns the set of URIs that were successfully processed
 * (partial failures do NOT abort the whole batch).
 */
object MediaActions {

    fun isSvgFile(item: MediaItem): Boolean {
        val ext = item.nome.substringAfterLast('.', "").lowercase()
        return ext == "svg" || ext == "svgz"
    }

    suspend fun deleteToTrash(items: List<MediaItem>): Map<String, TrashItem> =
        withContext(Dispatchers.Default) {
            val moved = mutableMapOf<String, TrashItem>()
            for (item in items) {
                yield()
                val trashItem = TrashManager.moveToTrash(item)
                if (trashItem != null) {
                    moved[item.uri.toString()] = trashItem
                } else {
                    VzLog.w("MediaActions", "Could not move to trash: ${item.uri}")
                }
            }
            moved
        }

    suspend fun deletePermanent(context: Context, items: List<MediaItem>): Set<String> =
        withContext(Dispatchers.Default) {
            val deleted = mutableSetOf<String>()
            for (item in items) {
                yield()
                try {
                    context.contentResolver.delete(item.uri, null, null)
                    deleted.add(item.uri.toString())
                } catch (e: Exception) {
                    VzLog.e("MediaActions", "Failed to permanently delete ${item.uri}", e)
                }
            }
            deleted
        }

    suspend fun hide(prefs: Prefs, uris: Set<String>) {
        prefs.hideItems(uris)
    }

    /**
     * Moves items to vault sequentially, yielding between items so the UI
     * thread can breathe. This fixes the "frozen screen" bug when moving
     * several files at once.
     */
    suspend fun moveToVault(context: Context, items: List<MediaItem>): Set<String> =
        withContext(Dispatchers.Default) {
            val moved = mutableSetOf<String>()
            for (item in items) {
                yield()
                val result = LockRepository.lockFile(context, item)
                if (result.isSuccess) {
                    moved.add(item.uri.toString())
                } else {
                    VzLog.e(
                        "MediaActions",
                        "Failed to move to vault: ${item.uri}",
                        result.exceptionOrNull()
                    )
                }
            }
            moved
        }

    fun share(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
        try {
            val uris = items.map { it.uri as android.os.Parcelable }
            val hasVideo = items.any { it.isVideo }
            val hasImage = items.any { !it.isVideo }
            val type = when {
                hasVideo && hasImage -> "*/*"
                hasVideo -> "video/*"
                else -> "image/*"
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                this.type = type
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Enviar via"))
        } catch (e: Exception) {
            VzLog.e("MediaActions", "Failed to share ${items.size} items", e)
        }
    }

    fun formatResult(okCount: Int, totalCount: Int, pastParticiple: String): String = when {
        okCount == 0 -> "Não foi possível concluir a ação"
        okCount == totalCount ->
            if (okCount == 1) "1 item $pastParticiple" else "$okCount itens ${pastParticiple}s"
        else -> "$okCount de $totalCount itens ${pastParticiple}s"
    }
}