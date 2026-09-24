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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import com.minusplayer.app.playback.PlaybackHistoryItem
import com.minusplayer.app.playback.PlaybackHistoryStore
import kotlinx.coroutines.delay

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
    historyStore: PlaybackHistoryStore,
    onRefreshLibrary: () -> Unit,
    onSelectFolder: () -> Unit,
    onOpenLibraryItem: (LibraryItem) -> Unit,
    onOpenHistoryItem: (PlaybackHistoryItem) -> Unit,
    onOpenSubtitleFile: () -> Unit = {},
    onSubtitleDownloaded: (android.net.Uri, String?, String?) -> Unit = { _, _, _ -> }
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
                when (selected) {
                    0 -> HomeScreen(
                        player = player,
                        historyStore = historyStore,
                        onOpenMedia = onOpenMedia,
                        onOpenHistoryItem = onOpenHistoryItem,
                        onToggleFullscreen = onToggleFullscreen,
                        onOpenSubtitleFile = onOpenSubtitleFile,
                        onSubtitleDownloaded = onSubtitleDownloaded,
                        modifier = Modifier.fillMaxSize()
                    )

                    1 -> LibraryScreen(
                        repository = libraryRepository,
                        filter = LibraryType.VIDEO,
                        onOpenItem = onOpenLibraryItem,
                        onSelectFolder = onSelectFolder,
                        onRefresh = onRefreshLibrary,
                        modifier = Modifier.fillMaxSize()
                    )

                    2 -> LibraryScreen(
                        repository = libraryRepository,
                        filter = LibraryType.AUDIO,
                        onOpenItem = onOpenLibraryItem,
                        onSelectFolder = onSelectFolder,
                        onRefresh = onRefreshLibrary,
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

@Composable
private fun HomeScreen(
    player: Player?,
    historyStore: PlaybackHistoryStore,
    onOpenMedia: () -> Unit,
    onOpenHistoryItem: (PlaybackHistoryItem) -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenSubtitleFile: () -> Unit,
    onSubtitleDownloaded: (android.net.Uri, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            refreshTick++
        }
    }

    val history = remember(refreshTick) { historyStore.recent(6) }

    Box(modifier.fillMaxSize()) {
        PlayerScreen(
            player = player,
            modifier = Modifier.fillMaxSize(),
            onToggleFullscreen = onToggleFullscreen,
            onOpenSubtitleFile = onOpenSubtitleFile,
            onSubtitleDownloaded = onSubtitleDownloaded
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onOpenMedia) {
                Icon(Icons.Default.FolderOpen, "Open media")
                Spacer(Modifier.width(8.dp))
                Text("Open")
            }
        }

        if (player?.currentMediaItem == null && history.isNotEmpty()) {
            ContinueWatchingPanel(
                items = history,
                onOpenItem = onOpenHistoryItem,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun ContinueWatchingPanel(
    items: List<PlaybackHistoryItem>,
    onOpenItem: (PlaybackHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xE6111111),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Continue watching",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.width(1.dp))
            items.forEach { item ->
                TextButton(
                    onClick = { onOpenItem(item) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(item.title, color = Color.White, maxLines = 1)
                        Text(
                            resumeLabel(item),
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun resumeLabel(item: PlaybackHistoryItem): String {
    if (item.durationMs <= 0L) return "Resume"
    val position = item.positionMs / 1000L
    val duration = item.durationMs / 1000L
    fun time(seconds: Long): String =
        if (seconds >= 3600L) {
            "%d:%02d:%02d".format(seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L)
        } else {
            "%02d:%02d".format(seconds / 60L, seconds % 60L)
        }
    return "Resume at ${time(position)} / ${time(duration)}"
}
