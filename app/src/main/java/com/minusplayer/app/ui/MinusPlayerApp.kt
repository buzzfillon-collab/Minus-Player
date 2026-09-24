package com.minusplayer.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player

private data class Destination(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val destinations = listOf(
    Destination("Home", Icons.Default.Home),
    Destination("Videos", Icons.Default.VideoLibrary),
    Destination("Music", Icons.Default.MusicNote),
    Destination("Folders", Icons.Default.Folder),
    Destination("Settings", Icons.Default.Settings)
)

@Composable
fun MinusPlayerApp(
    onOpenMedia: () -> Unit,
    player: Player?
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            destinations.forEachIndexed { index, destination ->
                NavigationRailItem(
                    selected = selected == index,
                    onClick = { selected = index },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    alwaysShowLabel = true
                )
            }
        }

        if (selected == 0) {
            MediaHome(
                onOpenMedia = onOpenMedia,
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
            )
        } else {
            MediaHome(
                onOpenMedia = onOpenMedia,
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
            )
        }
    }
}

@Composable
private fun MediaHome(
    onOpenMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Text(
            text = "Minus Player",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
        )
        androidx.compose.material3.Button(
            onClick = onOpenMedia,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Open media")
        }
    }
}
