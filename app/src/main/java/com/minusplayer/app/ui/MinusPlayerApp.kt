package com.minusplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

private data class Destination(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val destinations = listOf(
    Destination("Home", Icons.Default.Menu),
    Destination("Videos", Icons.Default.PlayArrow),
    Destination("Music", Icons.Default.VolumeUp),
    Destination("Folders", Icons.Default.FolderOpen),
    Destination("Settings", Icons.Default.Speed)
)

@OptIn(UnstableApi::class)
@Composable
fun MinusPlayerApp(
    onOpenMedia: () -> Unit,
    player: Player?
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Row(Modifier.fillMaxSize()) {
            if (selected != 0) {
                Column(
                    modifier = Modifier
                        .width(180.dp)
                        .fillMaxSize()
                        .background(Color(0xFF151515))
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "MINUS",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    destinations.drop(1).forEachIndexed { index, destination ->
                        androidx.compose.material3.TextButton(
                            onClick = { selected = index + 1 },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(destination.icon, null)
                            Spacer(Modifier.width(10.dp))
                            Text(destination.label)
                        }
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxSize()) {
                PlayerScreen(player = player, modifier = Modifier.fillMaxSize())

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onOpenMedia) {
                        Icon(Icons.Default.FolderOpen, "Open media", tint = Color.White)
                    }
                }

                if (selected == 0) {
                    PlayerControls(
                        player = player,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerControls(
    player: Player?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(MaterialTheme.shapes.medium),
        color = Color(0xDD111111)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { player?.seekToPreviousMediaItem() },
                    enabled = player != null
                ) {
                    Icon(Icons.Default.SkipPrevious, "Previous")
                }
                IconButton(
                    onClick = {
                        player?.let {
                            if (it.isPlaying) it.pause() else it.play()
                        }
                    },
                    enabled = player != null
                ) {
                    Icon(
                        if (player?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(
                    onClick = { player?.seekToNextMediaItem() },
                    enabled = player != null
                ) {
                    Icon(Icons.Default.SkipNext, "Next")
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = player?.let {
                        formatTime(it.currentPosition) + " / " + formatTime(it.duration)
                    } ?: "00:00 / 00:00",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )

                IconButton(onClick = {}) {
                    Icon(Icons.Default.VolumeUp, "Volume")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Fullscreen, "Fullscreen")
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
