package com.minusplayer.app.library

import android.net.Uri

enum class LibraryType { VIDEO, AUDIO }

data class LibraryItem(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val modifiedEpochSeconds: Long,
    val type: LibraryType
)
