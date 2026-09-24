package com.minusplayer.app.library

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executors

class LibraryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val database = LibraryDatabase(appContext)

    var items by mutableStateOf(database.readAll())
        private set
    var isScanning by mutableStateOf(false)
        private set
    var scanError by mutableStateOf<String?>(null)
        private set
    var selectedFolders by mutableStateOf(loadFolders())
        private set

    var lastScanEpochMillis by mutableStateOf(
        prefs.getLong(KEY_LAST_SCAN, 0L)
    )
        private set

    fun scanAll() {
        if (isScanning) return
        isScanning = true
        scanError = null
        executor.execute {
            try {
                val discovered = LinkedHashMap<String, LibraryItem>()
                scanMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, LibraryType.VIDEO, discovered)
                scanMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, LibraryType.AUDIO, discovered)
                selectedFolders.forEach { scanTree(Uri.parse(it), discovered) }
                val scannedItems = discovered.values.sortedBy { it.name.lowercase() }
                database.replaceAll(scannedItems)
                publish(scannedItems)
            } catch (t: Throwable) {
                mainHandler.post {
                    scanError = t.message ?: "Media scan failed"
                    isScanning = false
                }
            }
        }
    }

    fun addFolder(treeUri: Uri) {
        val value = treeUri.toString()
        if (selectedFolders.contains(value)) return
        runCatching {
            resolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        selectedFolders = (selectedFolders + value).distinct()
        saveFolders(selectedFolders)
    }

    fun removeFolder(treeUri: String) {
        selectedFolders = selectedFolders.filterNot { it == treeUri }
        saveFolders(selectedFolders)
    }

    fun shutdown() {
        executor.shutdownNow()
        database.close()
    }

    private fun scanMediaStore(
        collection: Uri,
        type: LibraryType,
        output: MutableMap<String, LibraryItem>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DURATION
        )

        resolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DISPLAY_NAME} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val uri = Uri.withAppendedPath(collection, id.toString())
                output[uri.toString()] = LibraryItem(
                    uri = uri,
                    name = cursor.getString(nameIndex) ?: "Unknown",
                    mimeType = cursor.getString(mimeIndex) ?: "",
                    sizeBytes = cursor.getLongOrZero(sizeIndex),
                    durationMs = cursor.getLongOrZero(durationIndex),
                    modifiedEpochSeconds = cursor.getLongOrZero(modifiedIndex),
                    type = type
                )
            }
        }
    }

    private fun scanTree(treeUri: Uri, output: MutableMap<String, LibraryItem>) {
        scanDocumentChildren(treeUri, DocumentsContract.getTreeDocumentId(treeUri), output)
    }

    private fun scanDocumentChildren(
        treeUri: Uri,
        parentDocumentId: String,
        output: MutableMap<String, LibraryItem>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null,
            null,
            "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val documentId = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex) ?: "Unknown"
                val mime = cursor.getString(mimeIndex) ?: ""

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    scanDocumentChildren(treeUri, documentId, output)
                    continue
                }

                val type = mediaType(name, mime) ?: continue
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                output[documentUri.toString()] = LibraryItem(
                    uri = documentUri,
                    name = name,
                    mimeType = mime,
                    sizeBytes = cursor.getLongOrZero(sizeIndex),
                    durationMs = 0L,
                    modifiedEpochSeconds = cursor.getLongOrZero(modifiedIndex) / 1000L,
                    type = type
                )
            }
        }
    }

    private fun mediaType(name: String, mime: String): LibraryType? {
        val lowerName = name.lowercase()
        return when {
            mime.startsWith("video/") || VIDEO_EXTENSIONS.any(lowerName::endsWith) -> LibraryType.VIDEO
            mime.startsWith("audio/") || AUDIO_EXTENSIONS.any(lowerName::endsWith) -> LibraryType.AUDIO
            else -> null
        }
    }

    private fun publish(newItems: List<LibraryItem>) {
        mainHandler.post {
            items = newItems
            lastScanEpochMillis = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_SCAN, lastScanEpochMillis).apply()
            isScanning = false
        }
    }

    private fun loadFolders(): List<String> =
        prefs.getStringSet(KEY_FOLDERS, emptySet())?.toList().orEmpty()

    private fun saveFolders(folders: List<String>) {
        prefs.edit().putStringSet(KEY_FOLDERS, folders.toSet()).apply()
    }

    companion object {
        private const val PREFS_NAME = "media_library"
        private const val KEY_FOLDERS = "selected_folders"
        private const val KEY_LAST_SCAN = "last_scan_epoch_millis"
        private val VIDEO_EXTENSIONS = setOf(".3gp", ".avi", ".flv", ".m2ts", ".m4v", ".mkv", ".mov", ".mp4", ".mpeg", ".mpg", ".ts", ".webm", ".wmv")
        private val AUDIO_EXTENSIONS = setOf(".aac", ".alac", ".flac", ".m4a", ".mp3", ".oga", ".ogg", ".opus", ".wav", ".weba", ".wma")
    }
}

private fun Cursor.getLongOrZero(index: Int): Long =
    if (isNull(index)) 0L else getLong(index)
