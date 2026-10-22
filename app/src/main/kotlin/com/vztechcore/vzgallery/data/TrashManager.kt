package com.vztechcore.vzgallery.data

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import com.vztechcore.vzgallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * VzGallery trash manager.
 *
 * Storage location:
 *  - Before: filesDir/trash                (internal, isolated)
 *  - Now:    getExternalFilesDir/trash     (external isolated — visible only to this app)
 *
 * The move to external files dir allows using [File.renameTo] instead of
 * copying bytes, so deleting a 50 MB video to trash is instant and the
 * app doesn't grow in size.
 *
 * Other galleries/apps CANNOT see this folder — the Android/data/<pkg>/
 * directory is isolated by the OS.
 *
 * Retention: 24h before permanent deletion.
 */
object TrashManager {

    private const val DIR_NAME = "trash"
    private const val META_FILE = "metadata.json"

    private var appContext: Context? = null

    private val _items = MutableStateFlow<List<TrashItem>>(emptyList())
    val items: StateFlow<List<TrashItem>> = _items.asStateFlow()

    // ============================================================
    // DIR RESOLUTION
    // ============================================================

    private fun trashDir(): File {
        val ctx = ctx()
        val external = ctx.getExternalFilesDir(null)
        return if (external != null) {
            File(external, DIR_NAME)
        } else {
            File(ctx.filesDir, DIR_NAME)
        }
    }

    private fun legacyTrashDir(): File = File(ctx().filesDir, DIR_NAME)

    private fun metaFile(): File = File(trashDir(), META_FILE)

    private fun ctx(): Context =
        appContext ?: error("TrashManager.init() must be called in Application.onCreate()")

    // ============================================================
    // INIT
    // ============================================================
    fun init(context: Context) {
        appContext = context.applicationContext
        val newDir = trashDir()
        if (!newDir.exists()) newDir.mkdirs()
        migrateLegacyTrashIfNeeded()
    }

    private fun migrateLegacyTrashIfNeeded() {
        try {
            val oldDir = legacyTrashDir()
            val newDir = trashDir()
            if (!oldDir.exists() || oldDir.absolutePath == newDir.absolutePath) return

            val oldMeta = File(oldDir, META_FILE)
            val newMeta = File(newDir, META_FILE)

            if (newMeta.exists() && newMeta.length() > 0) return

            oldDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val target = File(newDir, file.name)
                    val moved = file.renameTo(target)
                    if (!moved) {
                        try {
                            file.inputStream().use { input ->
                                FileOutputStream(target).use { output -> input.copyTo(output) }
                            }
                            file.delete()
                        } catch (_: Exception) {}
                    }
                }
            }

            if (oldMeta.exists()) {
                val moved = oldMeta.renameTo(newMeta)
                if (!moved) {
                    try {
                        oldMeta.inputStream().use { input ->
                            FileOutputStream(newMeta).use { output -> input.copyTo(output) }
                        }
                        oldMeta.delete()
                    } catch (_: Exception) {}
                }
            }

            try { oldDir.delete() } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    // ============================================================
    // MAIN OPERATIONS
    // ============================================================

    suspend fun moveToTrash(item: MediaItem): TrashItem? = withContext(Dispatchers.IO) {
        try {
            val originalPath = findAbsolutePath(item.uri) ?: buildPathFrom(item)
            val sourceFile = File(originalPath)

            val uuid = UUID.randomUUID().toString()
            val ext = item.nome.substringAfterLast('.', "").ifBlank {
                if (item.isVideo) "mp4" else "jpg"
            }
            val targetFile = File(trashDir(), "$uuid.$ext")

            // ===== Fast path: renameTo =====
            val moved = sourceFile.exists() && sourceFile.renameTo(targetFile)

            if (!moved) {
                // Slow fallback: copy bytes via ContentResolver
                val input = ctx().contentResolver.openInputStream(item.uri)
                if (input == null) {
                    if (!sourceFile.exists()) return@withContext null
                    sourceFile.inputStream().use { inp ->
                        FileOutputStream(targetFile).use { out -> inp.copyTo(out) }
                    }
                } else {
                    input.use { inp ->
                        FileOutputStream(targetFile).use { out -> inp.copyTo(out) }
                    }
                }

                try { ctx().contentResolver.delete(item.uri, null, null) }
                catch (_: Exception) {
                    try { sourceFile.delete() } catch (_: Exception) {}
                }
            } else {
                try {
                    MediaScannerConnection.scanFile(
                        ctx(), arrayOf(originalPath), null, null
                    )
                } catch (_: Exception) {}
            }

            val newItem = TrashItem(
                id = uuid,
                originalName = item.nome,
                originalPath = originalPath,
                relativePath = item.relativePath,
                trashFile = targetFile.absolutePath,
                isVideo = item.isVideo,
                sizeBytes = targetFile.length(),
                dateAdded = item.dataAdicionada,
                deletedAt = System.currentTimeMillis()
            )

            val current = loadSync().toMutableList()
            current.add(newItem)
            saveSync(current)
            _items.value = current

            newItem
        } catch (_: Exception) {
            null
        }
    }

    suspend fun restore(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val source = File(item.trashFile)
            if (!source.exists()) return@withContext false

            val target = File(item.originalPath)
            target.parentFile?.mkdirs()

            val moved = source.renameTo(target)

            if (!moved) {
                source.inputStream().use { inp ->
                    FileOutputStream(target).use { out -> inp.copyTo(out) }
                }
                source.delete()
            }

            MediaScannerConnection.scanFile(
                ctx(),
                arrayOf(target.absolutePath),
                null
            ) { _, _ -> }

            val current = loadSync().filterNot { it.id == item.id }
            saveSync(current)
            _items.value = current

            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deletePermanently(item: TrashItem) = withContext(Dispatchers.IO) {
        try {
            File(item.trashFile).delete()
            val current = loadSync().filterNot { it.id == item.id }
            saveSync(current)
            _items.value = current
        } catch (_: Exception) {}
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        try {
            loadSync().forEach { File(it.trashFile).delete() }
            saveSync(emptyList())
            _items.value = emptyList()
        } catch (_: Exception) {}
    }

    suspend fun purgeExpired() = withContext(Dispatchers.IO) {
        try {
            val current = loadSync()
            val (expired, valid) = current.partition { it.isExpired() }
            expired.forEach { File(it.trashFile).delete() }
            if (expired.isNotEmpty()) {
                saveSync(valid)
                _items.value = valid
            } else {
                _items.value = current
            }
        } catch (_: Exception) {}
    }

    suspend fun reload() = withContext(Dispatchers.IO) {
        _items.value = loadSync()
    }

    fun totalSize(): Long = _items.value.sumOf { it.sizeBytes }

    // ============================================================
    // INTERNAL — persistence
    // ============================================================

    private fun loadSync(): List<TrashItem> {
        val file = metaFile()
        if (!file.exists()) return emptyList()
        return try {
            val txt = file.readText()
            if (txt.isBlank()) return emptyList()
            val arr = JSONArray(txt)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val trashFile = o.getString("trashFile")
                if (!File(trashFile).exists()) return@mapNotNull null
                TrashItem(
                    id = o.getString("id"),
                    originalName = o.getString("originalName"),
                    originalPath = o.getString("originalPath"),
                    relativePath = o.optString("relativePath", ""),
                    trashFile = trashFile,
                    isVideo = o.getBoolean("isVideo"),
                    sizeBytes = o.optLong("sizeBytes", 0L),
                    dateAdded = o.optLong("dateAdded", 0L),
                    deletedAt = o.getLong("deletedAt")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveSync(list: List<TrashItem>) {
        try {
            val arr = JSONArray()
            list.forEach { item ->
                val o = JSONObject().apply {
                    put("id", item.id)
                    put("originalName", item.originalName)
                    put("originalPath", item.originalPath)
                    put("relativePath", item.relativePath)
                    put("trashFile", item.trashFile)
                    put("isVideo", item.isVideo)
                    put("sizeBytes", item.sizeBytes)
                    put("dateAdded", item.dateAdded)
                    put("deletedAt", item.deletedAt)
                }
                arr.put(o)
            }
            metaFile().writeText(arr.toString())
        } catch (_: Exception) {}
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private fun findAbsolutePath(uri: Uri): String? {
        return try {
            @Suppress("DEPRECATION")
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            ctx().contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun buildPathFrom(item: MediaItem): String {
        val base = "/storage/emulated/0/"
        val rel = item.relativePath.trimStart('/').trimEnd('/')
        return if (rel.isBlank()) "$base${item.nome}" else "$base/$rel/${item.nome}"
    }
}