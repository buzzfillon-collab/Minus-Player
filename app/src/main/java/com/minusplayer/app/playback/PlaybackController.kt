package com.minusplayer.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
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
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(uri.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(displayTitle)
                    .build()
            )
            .build()
        controller.setMediaItem(mediaItem, historyStore.getResumePosition(uri.toString()))
        controller.prepare()
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
