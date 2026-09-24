package com.minusplayer.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.media3.session.MediaController
import com.minusplayer.app.playback.PlaybackController
import com.minusplayer.app.ui.MinusPlayerApp
import com.minusplayer.app.ui.PlayerScreen
import com.minusplayer.app.ui.theme.MinusPlayerTheme

class MainActivity : ComponentActivity() {

    private lateinit var playbackController: PlaybackController
    private var mediaController by mutableStateOf<MediaController?>(null)

    private val openMediaLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            mediaController?.let { controller ->
                playbackController.setMediaItem(controller, uri)
                controller.play()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playbackController = PlaybackController(this)
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
                        onOpenMedia = {
                            openMediaLauncher.launch(
                                arrayOf(
                                    "video/*",
                                    "audio/*"
                                )
                            )
                        },
                        player = mediaController
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        mediaController = null
        playbackController.release()
        super.onDestroy()
    }
}
