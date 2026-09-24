package com.minusplayer.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.session.MediaController
import com.minusplayer.app.library.LibraryRepository
import com.minusplayer.app.library.LibraryItem
import com.minusplayer.app.permissions.PermissionManager
import com.minusplayer.app.playback.PlaybackController
import com.minusplayer.app.ui.MinusPlayerApp
import com.minusplayer.app.ui.theme.MinusPlayerTheme

class MainActivity : ComponentActivity() {

    private lateinit var playbackController: PlaybackController
    private lateinit var libraryRepository: LibraryRepository
    private var mediaController by mutableStateOf<MediaController?>(null)
    private var fullscreen = false
    private var pendingResumeUri: Uri? = null
    private var pendingResumePosition: Long = 0L

    private val openMediaLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            mediaController?.let { controller ->
                playbackController.setMediaItem(controller, uri)
                controller.seekTo(pendingResumePosition)
                controller.play()
                pendingResumeUri = null
                pendingResumePosition = 0L
            }
        }

    private val selectFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            libraryRepository.addFolder(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        playbackController = PlaybackController(this)
        libraryRepository = LibraryRepository(this)

        playbackController.controllerFuture.addListener(
            {
                mediaController = playbackController.controllerFuture.get()
            },
            ContextCompat.getMainExecutor(this)
        )

        setContent {
            MinusPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MinusPlayerApp(
                        onOpenMedia = ::openMediaWithPermission,
                        player = mediaController,
                        onToggleFullscreen = ::toggleFullscreen,
                        libraryRepository = libraryRepository,
                        onScanLibrary = ::scanLibraryWithPermission,
                        onSelectFolder = ::selectFolder,
                        onOpenLibraryItem = ::openLibraryItem
                    )
                }
            }
        }
    }

    private val requestMediaPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.all { it }) libraryRepository.scanAll()
        }

    private fun openMediaWithPermission() {
        val permissions = PermissionManager.mediaPermissions()
        if (permissions.isEmpty() || permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            openMediaPicker()
        } else {
            requestMediaPermissions.launch(permissions)
        }
    }

    private fun scanLibraryWithPermission() {
        val permissions = PermissionManager.mediaPermissions()
        if (permissions.isEmpty() || permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            libraryRepository.scanAll()
        } else {
            requestMediaPermissions.launch(permissions)
        }
    }

    private fun openMediaPicker() {
        openMediaLauncher.launch(arrayOf("video/*", "audio/*"))
    }

    private fun selectFolder() {
        selectFolderLauncher.launch(null)
    }

    private fun openLibraryItem(item: LibraryItem) {
        mediaController?.let { controller ->
            playbackController.setMediaItem(controller, item.uri)
            controller.play()
        }
    }

    private fun toggleFullscreen() {
        fullscreen = !fullscreen
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onDestroy() {
        mediaController = null
        playbackController.release()
        libraryRepository.shutdown()
        super.onDestroy()
    }
}
