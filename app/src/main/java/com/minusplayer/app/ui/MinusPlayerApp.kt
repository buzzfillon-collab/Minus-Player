package com.minusplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.minusplayer.app.library.LibraryItem
import com.minusplayer.app.library.LibraryRepository
import com.minusplayer.app.library.LibraryType

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

@Composable
fun MinusPlayerApp(
    onOpenMedia: () -> Unit,
    player: Player?,
    onToggleFullscreen: () -> Unit = {},
    libraryRepository: LibraryRepository,
    onScanLibrary: () -> Unit,
    onSelectFolder: () -> Unit,
    onOpenLibraryItem: (LibraryItem) -> Unit
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
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
                        TextButton(
                            onClick = {
                                selected = index + 1
                                if (selected == 1 || selected == 2) onScanLibrary()
                            },
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
                when (selected) {
                    0 -> {
                        PlayerScreen(
                            player = player,
                            modifier = Modifier.fillMaxSize(),
                            onToggleFullscreen = onToggleFullscreen
                        )
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
                    }

                    1 -> LibraryScreen(
                        repository = libraryRepository,
                        filter = LibraryType.VIDEO,
                        onOpenItem = onOpenLibraryItem,
                        onSelectFolder = onSelectFolder,
                        modifier = Modifier.fillMaxSize()
                    )

                    2 -> LibraryScreen(
                        repository = libraryRepository,
                        filter = LibraryType.AUDIO,
                        onOpenItem = onOpenLibraryItem,
                        onSelectFolder = onSelectFolder,
                        modifier = Modifier.fillMaxSize()
                    )

                    3 -> FolderScreen(
                        repository = libraryRepository,
                        onSelectFolder = onSelectFolder,
                        modifier = Modifier.fillMaxSize()
                    )

                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Settings", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
