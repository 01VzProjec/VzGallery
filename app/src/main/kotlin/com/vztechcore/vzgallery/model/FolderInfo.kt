package com.vztechcore.vzgallery.model

import android.net.Uri

data class FolderInfo(
    val relativePath: String,   // unique key: "DCIM/Camera/"
    val displayName: String,    // friendly name: "Camera"
    val fullPath: String,       // "DCIM/Camera" (no trailing slash)
    val itemCount: Int,
    val sampleUri: Uri?         // first media item, for preview
)