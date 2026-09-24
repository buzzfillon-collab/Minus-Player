package com.minusplayer.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.media3.common.C
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

        if (mimeType != null) builder.setMimeType(mimeType)

        // Automatically sideload an adjacent subtitle with the exact same base name,
        // e.g. Movie.mkv -> Movie.srt / Movie.ass / Movie.vtt.
        findSiblingSubtitle(uri)?.let { subtitleUri ->
            subtitleMimeType(subtitleUri)?.let { subtitleMime ->
                val subtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                    .setMimeType(subtitleMime)
                    .setLanguage(DEFAULT_LANGUAGE)
                    .setLabel("English")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                builder.setSubtitleConfigurations(listOf(subtitle))
            }
        }

        controller.setMediaItem(
            builder.build(),
            historyStore.getResumePosition(uri.toString())
        )
        controller.prepare()
    }

    private fun findSiblingSubtitle(mediaUri: Uri): Uri? {
        val displayName = queryDisplayName(mediaUri) ?: return null
        val dot = displayName.lastIndexOf('.')
        if (dot <= 0) return null
        val baseName = displayName.substring(0, dot)

        if (DocumentsContract.isDocumentUri(applicationContext, mediaUri) &&
            mediaUri.pathSegments.contains("tree")
        ) {
            runCatching {
                val treeUri = buildTreeUri(mediaUri) ?: return@runCatching null
                val parentId = DocumentsContract.getDocumentId(mediaUri)
                    .substringBeforeLast('/')
                    .takeIf { it.isNotBlank() }
                    ?: DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    parentId
                )
                applicationContext.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID
                    )
                    val nameIndex = cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    )
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex) ?: continue
                        if (nameWithoutExtension(name).equals(baseName, ignoreCase = true) &&
                            subtitleExtension(name)
                        ) {
                            return@use DocumentsContract.buildDocumentUriUsingTree(
                                treeUri,
                                cursor.getString(idIndex)
                            )
                        }
                    }
                }
            }.getOrNull()?.let { return it }
        }

        return queryMediaStoreSibling(mediaUri, baseName)
    }

    private fun queryMediaStoreSibling(mediaUri: Uri, baseName: String): Uri? {
        val sourcePath = runCatching {
            applicationContext.contentResolver.query(
                mediaUri,
                arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: return null

        val filesUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME
        )
        return runCatching {
            applicationContext.contentResolver.query(
                filesUri,
                projection,
                MediaStore.Files.FileColumns.RELATIVE_PATH + " = ?",
                arrayOf(sourcePath),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: continue
                    if (nameWithoutExtension(name).equals(baseName, ignoreCase = true) &&
                        subtitleExtension(name)
                    ) {
                        return@use MediaStore.Files.getContentUri(
                            MediaStore.VOLUME_EXTERNAL,
                            cursor.getLong(idIndex)
                        )
                    }
                }
            }
        }.getOrNull()
    }

    private fun queryDisplayName(uri: Uri): String? =
        applicationContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/')

    private fun buildTreeUri(documentUri: Uri): Uri? {
        val authority = documentUri.authority ?: return null
        val treeIndex = documentUri.pathSegments.indexOf("tree")
        if (treeIndex < 0 || treeIndex + 1 >= documentUri.pathSegments.size) return null
        return Uri.Builder()
            .scheme("content")
            .authority(authority)
            .appendPath("tree")
            .appendPath(documentUri.pathSegments[treeIndex + 1])
            .build()
    }

    private fun nameWithoutExtension(name: String): String =
        name.substringBeforeLast('.', name)

    private fun subtitleExtension(name: String): Boolean =
        when (name.substringAfterLast('.', "").lowercase()) {
            "srt", "ass", "ssa", "vtt", "ttml", "xml" -> true
            else -> false
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

    fun addExternalSubtitle(
        controller: MediaController,
        subtitleUri: Uri,
        language: String? = null,
        label: String? = null
    ) {
        val current = controller.currentMediaItem ?: return
        val mimeType = subtitleMimeType(subtitleUri)
            ?: throw IllegalArgumentException("Unsupported subtitle format.")

        val configuration = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(mimeType)
            .setLanguage(language?.takeIf { it.isNotBlank() })
            .setLabel(label?.takeIf { it.isNotBlank() })
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val existing = current.localConfiguration?.subtitleConfigurations.orEmpty()
        val updated = current.buildUpon()
            .setSubtitleConfigurations(existing + configuration)
            .build()
        val position = controller.currentPosition.coerceAtLeast(0L)
        val wasPlaying = controller.playWhenReady
        controller.setMediaItem(updated, position)
        controller.prepare()
        controller.playWhenReady = wasPlaying
        if (wasPlaying) controller.play()
    }

    private fun subtitleMimeType(uri: Uri): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString()).lowercase()
        return when (extension) {
            "srt" -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
            "vtt" -> androidx.media3.common.MimeTypes.TEXT_VTT
            "ass", "ssa" -> androidx.media3.common.MimeTypes.TEXT_SSA
            "ttml", "xml" -> androidx.media3.common.MimeTypes.APPLICATION_TTML
            else -> applicationContext.contentResolver.getType(uri)?.let {
                when {
                    it.equals("text/vtt", true) -> androidx.media3.common.MimeTypes.TEXT_VTT
                    it.equals("application/x-subrip", true) -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
                    it.equals("text/x-ssa", true) -> androidx.media3.common.MimeTypes.TEXT_SSA
                    it.equals("application/ttml+xml", true) -> androidx.media3.common.MimeTypes.APPLICATION_TTML
                    else -> null
                }
            }
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

    companion object {
        private const val DEFAULT_LANGUAGE = "en"
    }
}
