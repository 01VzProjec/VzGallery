package com.vztechcore.vzgallery.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val nome: String,
    val dataAdicionada: Long,
    val isVideo: Boolean = false,
    val duracaoMs: Long? = null,
    val relativePath: String = "",
    val tamanhoBytes: Long = 0L
)

/** Returns true if this is a video (checked by flag or extension). */
val MediaItem.ehVideo: Boolean
    get() {
        if (isVideo) return true
        val ext = nome.substringAfterLast('.', "").lowercase()
        return ext in setOf(
            "mp4", "mkv", "mov", "webm", "avi", "3gp", "m4v",
            "flv", "wmv", "mpeg", "mpg", "ts", "m2ts", "ogv"
        )
    }