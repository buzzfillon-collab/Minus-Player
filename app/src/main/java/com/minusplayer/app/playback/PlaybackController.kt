package com.minusplayer.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class PlaybackController(
    context: Context,
    private val historyStore: PlaybackHistoryStore
) {
    private val applicationContext = context.applicationContext
    private val sessionToken = SessionToken(
        applicationContext,
        ComponentName(applicationContext, PlaybackService::class.java)
    )

    val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(applicationContext, sessionToken).buildAsync()

    fun setMediaItem(controller: MediaController, uri: Uri, title: String? = null) {
        val displayTitle = title?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
            ?: uri.toString()

        val mimeType = resolveMimeType(uri)
        val builder = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(uri.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(displayTitle)
                    .build()
            )

        if (mimeType != null) {
            builder.setMimeType(mimeType)
        }

        controller.setMediaItem(
            builder.build(),
            historyStore.getResumePosition(uri.toString())
        )
        controller.prepare()
    }

    private fun resolveMimeType(uri: Uri): String? {
        val resolverType = applicationContext.contentResolver.getType(uri)
            ?.takeUnless { it.equals("application/octet-stream", ignoreCase = true) }
        if (resolverType != null) return resolverType

        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            .trim()
            .lowercase()
        return extension.takeIf { it.isNotEmpty() }?.let {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
        } ?: when (extension) {
            "mkv" -> "video/x-matroska"
            "m2ts" -> "video/mp2t"
            "ts" -> "video/mp2t"
            "avi" -> "video/x-msvideo"
            "flv" -> "video/x-flv"
            "ogm" -> "video/ogg"
            "wma" -> "audio/x-ms-wma"
            "weba" -> "audio/webm"
            else -> null
        }
    }

    fun retry(controller: MediaController) {
        controller.prepare()
        controller.play()
    }

    fun seekBy(controller: Player, offsetMs: Long) {
        controller.seekTo(
            (controller.currentPosition + offsetMs).coerceAtLeast(0L)
        )
    }

    fun release() {
        MediaController.releaseFuture(controllerFuture)
    }
}
