package com.vztechcore.vzgallery.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import com.vztechcore.vzgallery.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext

object StorageAnalyzer {

    private const val SAMPLE_SIZE = 64 * 1024

    /**
     * Quantos bits podem diferir entre dois hashes perceptuais
     * pra ainda serem considerados "a mesma foto".
     *
     * 0  = idênticas visualmente (mas pode ter compressão diferente)
     * 5  = parecidas (crop leve, compressão diferente, resize)
     * 10 = vagamente parecidas
     * 15 = praticamente diferente
     *
     * 5 é o sweet spot pra galerias — pega duplicatas reais sem falsos positivos.
     */
    private const val PHASH_THRESHOLD = 5

    data class DuplicateGroup(
        val hash: String,
        val items: List<MediaItem>,
        val wastedBytes: Long
    ) {
        val realItem: MediaItem?
            get() = items.minByOrNull { it.dataAdicionada }

        val duplicateItems: List<MediaItem>
            get() {
                val real = realItem ?: return emptyList()
                val realUri = real.uri.toString()
                return items.filter { it.uri.toString() != realUri }
            }
    }

    data class AnalysisResult(
        val totalStorageBytes: Long,
        val freeStorageBytes: Long,
        val usedStorageBytes: Long,
        val mediaBytes: Long,
        val photoCount: Int,
        val videoCount: Int,
        val screenshots: List<MediaItem>,
        val screenshotBytes: Long,
        val duplicatePhotos: List<DuplicateGroup>,
        val duplicateVideos: List<DuplicateGroup>,
        val duplicateSvgs: List<DuplicateGroup>,
        val duplicatePhotoBytes: Long,
        val duplicateVideoBytes: Long,
        val duplicateSvgBytes: Long,
        val canFreeBytes: Long,
        val cantFreeBytes: Long,
    )

    /**
     * Analysis order:
     *   1. SVGs        (may be skipped via [shouldSkipSvg])
     *   2. Photos      (perceptual hash — catches visual duplicates)
     *   3. Videos      (byte-identical hash — fast)
     */
    suspend fun analyze(
        context: Context,
        items: List<MediaItem>,
        shouldSkipSvg: () -> Boolean = { false },
        onSvgPhaseEnd: () -> Unit = {},
        onProgress: (processed: Int, total: Int, currentItem: MediaItem?) -> Unit
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        // ===== Storage =====
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val totalStorage = stat.blockCountLong * stat.blockSizeLong
        val freeStorage = stat.availableBlocksLong * stat.blockSizeLong
        val usedStorage = (totalStorage - freeStorage).coerceAtLeast(0L)

        // ===== Split by type =====
        val photos = items.filter { !it.isVideo && !isSvg(it) }
        val videos = items.filter { it.isVideo }
        val svgs = items.filter { isSvg(it) }

        val screenshots = photos.filter { isScreenshot(it) }
        val screenshotUris = screenshots.map { it.uri.toString() }.toSet()

        val mediaBytes = photos.sumOf { it.tamanhoBytes } + videos.sumOf { it.tamanhoBytes }
        val svgBytes = svgs.sumOf { it.tamanhoBytes }
        val totalMedia = mediaBytes + svgBytes

        val totalWork = svgs.size + photos.size + videos.size
        var processed = 0
        val tick: (MediaItem) -> Unit = { item ->
            processed++
            onProgress(processed, totalWork, item)
        }

        // ==================================================
        // 1. SVG DUPLICATES
        // ==================================================
        val dupSvgs = findSvgDuplicates(context, svgs, coroutineContext, shouldSkipSvg, tick)
        onSvgPhaseEnd()

        // ==================================================
        // 2. PHOTO DUPLICATES — perceptual hash
        //    (excluding screenshots — they have their own section)
        // ==================================================
        val photosForDup = photos.filterNot { it.uri.toString() in screenshotUris }
        val dupPhotos = findPerceptualDuplicates(context, photosForDup, coroutineContext, tick)

        // ==================================================
        // 3. VIDEO DUPLICATES — byte-identical (fast)
        // ==================================================
        val dupVideos = findByteDuplicates(context, videos, coroutineContext, tick)

        val dupPhotoBytes = dupPhotos.sumOf { it.wastedBytes }
        val dupVideoBytes = dupVideos.sumOf { it.wastedBytes }
        val dupSvgBytes = dupSvgs.sumOf { it.wastedBytes }
        val screenshotBytes = screenshots.sumOf { it.tamanhoBytes }

        val canFree = screenshotBytes + dupPhotoBytes + dupVideoBytes + dupSvgBytes
        val cantFree = (totalMedia - canFree).coerceAtLeast(0L)

        AnalysisResult(
            totalStorageBytes = totalStorage,
            freeStorageBytes = freeStorage,
            usedStorageBytes = usedStorage,
            mediaBytes = mediaBytes,
            photoCount = photos.size,
            videoCount = videos.size,
            screenshots = screenshots,
            screenshotBytes = screenshotBytes,
            duplicatePhotos = dupPhotos,
            duplicateVideos = dupVideos,
            duplicateSvgs = dupSvgs,
            duplicatePhotoBytes = dupPhotoBytes,
            duplicateVideoBytes = dupVideoBytes,
            duplicateSvgBytes = dupSvgBytes,
            canFreeBytes = canFree,
            cantFreeBytes = cantFree,
        )
    }

    // ============================================================
    // PERCEPTUAL HASH — aHash 8x8 (catches visual duplicates)
    // ============================================================
    private fun findPerceptualDuplicates(
        context: Context,
        items: List<MediaItem>,
        coroutineContext: CoroutineContext,
        onTick: (MediaItem) -> Unit
    ): List<DuplicateGroup> {
        if (items.isEmpty()) return emptyList()

        data class Hashed(val item: MediaItem, val hash: Long)

        // 1. Compute aHash for every photo
        val hashed = ArrayList<Hashed>(items.size)
        items.forEach { item ->
            coroutineContext.ensureActive()
            onTick(item)
            val h = computePerceptualHash(context, item.uri) ?: return@forEach
            hashed.add(Hashed(item, h))
        }

        if (hashed.size < 2) return emptyList()

        // 2. Union-find: compare every pair (n² over 64-bit XOR is fast)
        val parent = IntArray(hashed.size) { it }
        fun find(x: Int): Int {
            var i = x
            while (parent[i] != i) {
                parent[i] = parent[parent[i]]
                i = parent[i]
            }
            return i
        }
        fun union(a: Int, b: Int) {
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) parent[ra] = rb
        }

        for (i in hashed.indices) {
            for (j in i + 1 until hashed.size) {
                val dist = java.lang.Long.bitCount(hashed[i].hash xor hashed[j].hash)
                if (dist <= PHASH_THRESHOLD) {
                    union(i, j)
                }
            }
            // Check for cancellation periodically
            if (i % 32 == 0) coroutineContext.ensureActive()
        }

        // 3. Group by root
        val groups = mutableMapOf<Int, MutableList<MediaItem>>()
        for (i in hashed.indices) {
            val r = find(i)
            groups.getOrPut(r) { mutableListOf() }.add(hashed[i].item)
        }

        // 4. Keep only groups with 2+ items
        return groups.values
            .filter { it.size >= 2 }
            .map { list ->
                val sorted = list.sortedByDescending { it.dataAdicionada }
                val wasted = sorted.drop(1).sumOf { it.tamanhoBytes }
                val fakeHash = sorted.first().uri.toString()
                DuplicateGroup(fakeHash, sorted, wasted)
            }
            .sortedByDescending { it.wastedBytes }
    }

    /**
     * aHash: decode → 8x8 grayscale → bit = (pixel >= average).
     * Returns a 64-bit fingerprint, or null if decoding fails.
     */
    private fun computePerceptualHash(context: Context, uri: Uri): Long? {
        return try {
            // 1. Decode at very low resolution (fast) with low-memory config
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 8
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            } ?: return null

            // 2. Scale to exactly 8x8
            val small = Bitmap.createScaledBitmap(decoded, 8, 8, true)
            if (small !== decoded) decoded.recycle()

            // 3. Grayscale + average
            val gray = IntArray(64)
            var sum = 0
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val p = small.getPixel(x, y)
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    val lum = (r * 299 + g * 587 + b * 114) / 1000
                    gray[y * 8 + x] = lum
                    sum += lum
                }
            }
            small.recycle()

            val avg = sum / 64

            // 4. Build 64-bit hash
            var hash = 0L
            for (i in 0 until 64) {
                if (gray[i] >= avg) hash = hash or (1L shl i)
            }
            hash
        } catch (_: Exception) {
            null
        }
    }

    // ============================================================
    // BYTE-IDENTICAL (used for videos)
    // ============================================================
    private fun findByteDuplicates(
        context: Context,
        items: List<MediaItem>,
        coroutineContext: CoroutineContext,
        onTick: (MediaItem) -> Unit
    ): List<DuplicateGroup> {
        val bySize = items
            .filter { it.tamanhoBytes > 0 }
            .groupBy { it.tamanhoBytes }
            .filterValues { it.size >= 2 }

        val result = mutableListOf<DuplicateGroup>()

        bySize.values.forEach { sameSizeGroup ->
            val byHash = mutableMapOf<String, MutableList<MediaItem>>()
            sameSizeGroup.forEach { item ->
                coroutineContext.ensureActive()
                onTick(item)
                val hash = computeByteHash(context, item.uri, item.tamanhoBytes) ?: return@forEach
                byHash.getOrPut(hash) { mutableListOf() }.add(item)
            }
            byHash.forEach { (hash, group) ->
                if (group.size >= 2) {
                    val wasted = group.first().tamanhoBytes * (group.size - 1)
                    result.add(DuplicateGroup(hash, group, wasted))
                }
            }
        }

        // Tick items that didn't belong to a same-size group (or had size <= 0),
        // so the progress counter reaches 100%.
        items.filter { it.tamanhoBytes <= 0 || bySize[it.tamanhoBytes] == null }
            .forEach { item ->
                coroutineContext.ensureActive()
                onTick(item)
            }

        return result.sortedByDescending { it.wastedBytes }
    }

    /**
     * Fast fingerprint for byte-identical files (used for videos):
     *   hash( fileSize + first 64KB + last 64KB )
     *
     * Two different videos sharing the same size AND the same first/last
     * 64KB is effectively impossible, so this is safe and avoids hashing
     * gigabytes of bytes. We seek directly to the last block via the file
     * descriptor channel (fast), with a graceful fallback if the stream
     * is not seekable (then we keep the first-block hash only).
     */
    private fun computeByteHash(
        context: Context,
        uri: Uri,
        size: Long
    ): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            md.update(size.toString().toByteArray())

            val sample = ByteArray(SAMPLE_SIZE)

            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    // First block
                    val firstRead = fis.read(sample)
                    if (firstRead > 0) md.update(sample, 0, firstRead)

                    // Last block (only if the file is big enough to have two distinct blocks)
                    if (size > SAMPLE_SIZE * 2) {
                        try {
                            fis.channel.position(size - SAMPLE_SIZE)
                            val lastRead = fis.read(sample)
                            if (lastRead > 0) md.update(sample, 0, lastRead)
                        } catch (_: Exception) {
                            // Not seekable — keep first-block hash. Better to
                            // fingerprint weakly than to skip the item entirely.
                        }
                    }
                }
            } ?: return null

            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    // ============================================================
    // SVG DUPLICATES — compares <path d="..."> only
    // ============================================================
    private fun findSvgDuplicates(
        context: Context,
        items: List<MediaItem>,
        coroutineContext: CoroutineContext,
        shouldSkip: () -> Boolean,
        onTick: (MediaItem) -> Unit
    ): List<DuplicateGroup> {
        val byHash = mutableMapOf<String, MutableList<MediaItem>>()
        var skippedMidway = false

        items.forEach { item ->
            coroutineContext.ensureActive()
            onTick(item)
            if (shouldSkip()) {
                skippedMidway = true
                return@forEach
            }
            val hash = computeSvgPathHash(context, item.uri) ?: return@forEach
            byHash.getOrPut(hash) { mutableListOf() }.add(item)
        }

        if (skippedMidway) return emptyList()

        return byHash
            .filterValues { it.size >= 2 }
            .map { (hash, group) ->
                val wasted = group.drop(1).sumOf { it.tamanhoBytes }
                DuplicateGroup(hash, group, wasted)
            }
            .sortedByDescending { it.wastedBytes }
    }

    private fun computeSvgPathHash(context: Context, uri: Uri): String? {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return null

            val pathRegex = Regex(
                """<path\b[^>]*?\sd\s*=\s*["']([^"']+)["']""",
                RegexOption.IGNORE_CASE
            )
            val paths = pathRegex.findAll(text)
                .map { it.groupValues[1].trim() }
                .toList()

            if (paths.isEmpty()) return null

            val md = MessageDigest.getInstance("MD5")
            paths.sorted().forEach { md.update(it.toByteArray()) }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private fun isSvg(item: MediaItem): Boolean {
        val ext = item.nome.substringAfterLast('.', "").lowercase()
        return ext == "svg" || ext == "svgz"
    }

    private fun isScreenshot(item: MediaItem): Boolean {
        if (item.isVideo) return false
        val path = item.relativePath.lowercase()
        return path.contains("screenshot") || path.contains("capturas de tela")
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.0f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.2f GB".format(gb)
    }

    fun splitBytes(bytes: Long): Pair<String, String> {
        if (bytes <= 0) return "0" to "MB"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.0f".format(kb) to "KB"
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f".format(mb) to "MB"
        val gb = mb / 1024.0
        return "%.2f".format(gb) to "GB"
    }
}
