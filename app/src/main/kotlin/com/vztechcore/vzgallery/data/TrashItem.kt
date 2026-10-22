package com.vztechcore.vzgallery.data

/**
 * Item that was moved to trash. Stays here for 24h before being
 * permanently deleted, unless the user restores or deletes it manually.
 */
data class TrashItem(
    val id: String,
    val originalName: String,
    val originalPath: String,       // absolute path on disk
    val relativePath: String,       // relative folder (e.g. DCIM/Camera/)
    val trashFile: String,          // absolute path inside trash folder
    val isVideo: Boolean,
    val sizeBytes: Long,
    val dateAdded: Long,
    val deletedAt: Long,
) {
    /** Returns true if the 24h retention has passed. */
    fun isExpired(): Boolean =
        System.currentTimeMillis() - deletedAt >= RETENTION_MS

    /** Remaining time in milliseconds (0 if already expired). */
    fun remainingMs(): Long =
        (RETENTION_MS - (System.currentTimeMillis() - deletedAt)).coerceAtLeast(0L)

    companion object {
        const val RETENTION_MS = 24L * 60L * 60L * 1000L  // 24 hours
    }
}