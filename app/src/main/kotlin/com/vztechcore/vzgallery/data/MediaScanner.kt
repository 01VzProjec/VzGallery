package com.vztechcore.vzgallery.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.vztechcore.vzgallery.model.FolderInfo
import com.vztechcore.vzgallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val VIDEO_EXTS = setOf(
    "mp4", "mkv", "mov", "webm", "avi", "3gp", "m4v",
    "flv", "wmv", "mpeg", "mpg", "ts", "m2ts", "ogv"
)

// Acceptable date range (seconds since 1970): 2000-01-01 -> 2100-01-01
private const val MIN_VALID_SEG = 946_684_800L
private const val MAX_VALID_SEG = 4_102_444_800L

class MediaScanner(private val context: Context) {

    suspend fun loadMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = loadImages()
        val videos = loadVideos()
        (images + videos)
            .distinctBy { it.uri.toString() }
            .sortedByDescending { it.dataAdicionada }
    }

    private fun isVideoByExtension(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTS
    }

    private fun getRelativePath(
        relativePathCol: String?,
        dataCol: String?
    ): String {
        if (!relativePathCol.isNullOrBlank()) {
            return relativePathCol.replace('\\', '/')
        }
        if (!dataCol.isNullOrBlank()) {
            val file = File(dataCol)
            val parent = file.parent ?: return ""
            val idx = parent.indexOf("/storage/emulated/0/")
            val rel = if (idx >= 0) {
                parent.substring(idx + "/storage/emulated/0/".length)
            } else {
                parent
            }
            return if (rel.endsWith("/")) rel else "$rel/"
        }
        return ""
    }

    /**
     * Normalizes a MediaStore timestamp to SECONDS.
     * Returns -1 for garbage (0, negative, or out of 2000→2100 range).
     *
     * Accepts values in:
     *  - seconds (normal case of DATE_ADDED / DATE_MODIFIED)
     *  - milliseconds (DATE_TAKEN for camera photos)
     */
    private fun normalizeTimestamp(v: Long): Long {
        if (v <= 0) return -1L
        return when {
            v in MIN_VALID_SEG..MAX_VALID_SEG -> v
            v in (MIN_VALID_SEG * 1000L)..(MAX_VALID_SEG * 1000L) -> v / 1000L
            else -> -1L
        }
    }

    /**
     * Resolves the best available date, trying in order:
     *   1) DATE_TAKEN    (camera photos, in ms)
     *   2) DATE_ADDED    (almost always seconds)
     *   3) DATE_MODIFIED (seconds)
     *   4) file lastModified on disk
     *   5) current time (last resort)
     *
     * Always returns SECONDS.
     */
    private fun resolveDateAdded(
        cursor: Cursor,
        colTaken: Int,
        colAdded: Int,
        colMod: Int,
        filePath: String?
    ): Long {
        val columns = intArrayOf(colTaken, colAdded, colMod)
        for (col in columns) {
            if (col < 0) continue
            val raw = cursor.getLong(col)
            val norm = normalizeTimestamp(raw)
            if (norm > 0) return norm
        }
        // Fallback: real file date on disk
        if (!filePath.isNullOrBlank()) {
            try {
                val lm = File(filePath).lastModified() / 1000L
                if (lm in MIN_VALID_SEG..MAX_VALID_SEG) return lm
            } catch (_: Exception) { /* ignore */ }
        }
        // Last resort
        return System.currentTimeMillis() / 1000L
    }

    private fun loadImages(): List<MediaItem> {
        val list = mutableListOf<MediaItem>()

        // DATA is always queried (deprecated but still populated, and
        // required to derive the relative folder on API < 29).
        // RELATIVE_PATH is only added on API >= 29 (older MediaStore
        // tables don't have the column).
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Images.Media.RELATIVE_PATH)
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection.toTypedArray(), null, null, sortOrder
        )?.use { cursor ->
            val idCol        = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol      = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddCol   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val dateModCol   = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
            val mimeCol      = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeCol      = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
            val relCol       = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH) // -1 on API < 29
            val dataCol      = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idCol)
                val name     = cursor.getString(nameCol) ?: "sem_nome"
                val mime     = cursor.getString(mimeCol) ?: ""
                val relPath  = if (relCol >= 0) cursor.getString(relCol) else null
                val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                val rel      = getRelativePath(relPath, dataPath)
                val size     = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L

                val data = resolveDateAdded(
                    cursor   = cursor,
                    colTaken = dateTakenCol,
                    colAdded = dateAddCol,
                    colMod   = dateModCol,
                    filePath = dataPath
                )

                val isVideoHere = isVideoByExtension(name) || mime.startsWith("video/")

                list.add(
                    MediaItem(
                        id             = id,
                        uri            = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                        ),
                        nome           = name,
                        dataAdicionada = data,
                        isVideo        = isVideoHere,
                        relativePath   = rel,
                        tamanhoBytes   = size
                    )
                )
            }
        }

        return list
    }

    private fun loadVideos(): List<MediaItem> {
        // Videos do NOT have DATE_TAKEN in MediaStore. Use DATE_ADDED -> DATE_MODIFIED.
        val list = mutableListOf<MediaItem>()

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Video.Media.RELATIVE_PATH)
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection.toTypedArray(), null, null, sortOrder
        )?.use { cursor ->
            val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateAddCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
            val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol    = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val relCol     = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH) // -1 on API < 29
            val dataCol    = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idCol)
                val name     = cursor.getString(nameCol) ?: "sem_nome"
                val relPath  = if (relCol >= 0) cursor.getString(relCol) else null
                val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                val rel      = getRelativePath(relPath, dataPath)
                val size     = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L

                val data = resolveDateAdded(
                    cursor   = cursor,
                    colTaken = -1,
                    colAdded = dateAddCol,
                    colMod   = dateModCol,
                    filePath = dataPath
                )

                list.add(
                    MediaItem(
                        id             = id,
                        uri            = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        ),
                        nome           = name,
                        dataAdicionada = data,
                        isVideo        = true,
                        duracaoMs      = cursor.getLong(durCol),
                        relativePath   = rel,
                        tamanhoBytes   = size
                    )
                )
            }
        }

        return list
    }

    suspend fun listFolders(): List<FolderInfo> = withContext(Dispatchers.IO) {
        val allItems = loadMedia()
        allItems
            .groupBy { it.relativePath }
            .filter { it.key.isNotBlank() }
            .map { (path, items) ->
                val noSlash = path.trimEnd('/')
                val name = noSlash.substringAfterLast('/').ifBlank { noSlash }
                FolderInfo(
                    relativePath = path,
                    displayName  = name,
                    fullPath     = noSlash,
                    itemCount    = items.size,
                    sampleUri    = items.firstOrNull()?.uri
                )
            }
            .sortedByDescending { it.itemCount }
    }
}
