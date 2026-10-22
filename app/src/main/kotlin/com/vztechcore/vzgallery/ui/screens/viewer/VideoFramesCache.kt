package com.vztechcore.vzgallery.ui.screens.viewer

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max

object VideoFramesCache {

    private const val MIN_SECONDS_PER_FRAME = 3.0
    private const val MAX_FRAMES = 60
    private const val MIN_FRAMES = 2

    private val cache = mutableMapOf<String, List<Bitmap?>>()

    fun peek(uri: Uri): List<Bitmap?>? = cache[uri.toString()]

    fun clearAll() { cache.clear() }

    /**
     * Interval in seconds between each frame:
     * - always >= 3s
     * - grows for long videos (to fit within MAX_FRAMES)
     */
    private fun intervalSeconds(durationSeconds: Double): Double =
        max(MIN_SECONDS_PER_FRAME, durationSeconds / MAX_FRAMES)

    fun frameCountFor(durationMs: Long): Int {
        if (durationMs <= 0) return MIN_FRAMES
        val seconds = durationMs / 1000.0
        val interval = intervalSeconds(seconds)
        return ceil(seconds / interval).toInt().coerceIn(MIN_FRAMES, MAX_FRAMES)
    }

    suspend fun get(
        context: Context,
        uri: Uri,
        durationMs: Long
    ): List<Bitmap?> = withContext(Dispatchers.IO) {
        val key = uri.toString()
        cache[key]?.let { return@withContext it }

        val numFrames = frameCountFor(durationMs)
        val retriever = MediaMetadataRetriever()
        val frames = try {
            retriever.setDataSource(context, uri)
            (0 until numFrames).map { i ->
                val timeMs = if (durationMs > 0) {
                    (durationMs * i / (numFrames - 1).coerceAtLeast(1))
                } else 0L
                // OPTION_CLOSEST_SYNC uses keyframes — 10x faster than CLOSEST
                // on long videos, and still returns a representative frame.
                retriever.getFrameAtTime(
                    timeMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            }
        } catch (_: Exception) {
            List(numFrames) { null }
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        cache[key] = frames
        frames
    }
}