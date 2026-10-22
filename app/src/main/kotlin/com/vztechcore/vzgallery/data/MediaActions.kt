package com.vztechcore.vzgallery.data

import android.content.Context
import android.content.Intent
import com.vztechcore.vzgallery.model.MediaItem

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

    /**
     * Moves items to trash. Returns a map of [uri -> TrashItem] for successful moves.
     */
    suspend fun deleteToTrash(items: List<MediaItem>): Map<String, TrashItem> {
        val moved = mutableMapOf<String, TrashItem>()
        for (item in items) {
            val trashItem = TrashManager.moveToTrash(item)
            if (trashItem != null) {
                moved[item.uri.toString()] = trashItem
            }
        }
        return moved
    }

    /**
     * Permanently deletes items from MediaStore. Returns the set of deleted URIs.
     */
    suspend fun deletePermanent(context: Context, items: List<MediaItem>): Set<String> {
        val deleted = mutableSetOf<String>()
        for (item in items) {
            try {
                context.contentResolver.delete(item.uri, null, null)
                deleted.add(item.uri.toString())
            } catch (_: Exception) { /* try next */ }
        }
        return deleted
    }

    /**
     * Hides items (only removes from main list, does not delete files).
     */
    suspend fun hide(prefs: Prefs, uris: Set<String>) {
        prefs.hideItems(uris)
    }

    /**
     * Moves items to vault. Returns the set of moved URIs.
     */
    suspend fun moveToVault(context: Context, items: List<MediaItem>): Set<String> {
        val moved = mutableSetOf<String>()
        for (item in items) {
            val result = LockRepository.lockFile(context, item)
            if (result.isSuccess) {
                moved.add(item.uri.toString())
            }
        }
        return moved
    }

    /**
     * Opens a share sheet for the given items.
     */
    fun share(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
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
    }

    /**
     * Formats the result message for partial/full successes.
     * Example: formatResult(3, 5, "movido pro cofre") -> "3 de 5 itens movidos pro cofre"
     */
    fun formatResult(okCount: Int, totalCount: Int, pastParticiple: String): String = when {
        okCount == 0 -> "Não foi possível concluir a ação"
        okCount == totalCount ->
            if (okCount == 1) "1 item $pastParticiple" else "$okCount itens ${pastParticiple}s"
        else -> "$okCount de $totalCount itens ${pastParticiple}s"
    }
}