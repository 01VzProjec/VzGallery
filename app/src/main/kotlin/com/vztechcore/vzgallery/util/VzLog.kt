package com.vztechcore.vzgallery.util

import android.util.Log

/**
 * Centralized logging for VzGallery.
 *
 * Why a wrapper around Log?
 *  - Consistent tag prefix ("VzGallery.MediaActions" instead of "MediaActions")
 *  - Easy to disable in release builds by editing this one file
 *  - Easy to swap for Timber/Crashlytics in the future
 *
 * Usage:
 *   VzLog.e("MediaActions", "Failed to delete ${uri}", e)
 *   VzLog.w("TrashManager", "File not found, skipping")
 *   VzLog.d("Viewer", "Opened video")
 */
object VzLog {
    private const val TAG_PREFIX = "VzGallery"

    fun d(tag: String, message: String) {
        Log.d("$TAG_PREFIX.$tag", message)
    }

    fun w(tag: String, message: String, tr: Throwable? = null) {
        Log.w("$TAG_PREFIX.$tag", message, tr)
    }

    fun e(tag: String, message: String, tr: Throwable? = null) {
        Log.e("$TAG_PREFIX.$tag", message, tr)
    }
}