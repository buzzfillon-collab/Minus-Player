package com.minusplayer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minusplayer.app.library.LibraryItem
import com.minusplayer.app.library.LibraryRepository
import com.minusplayer.app.library.LibraryType
import android.net.Uri
import android.provider.DocumentsContract

@Composable
fun LibraryScreen(
    repository: LibraryRepository,
    filter: LibraryType? = null,
    onOpenItem: (LibraryItem) -> Unit,
    onSelectFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleItems = filter?.let { type -> repository.items.filter { it.type == type } } ?: repository.items

    Column(modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (filter) {
                        LibraryType.VIDEO -> "Videos"
                        LibraryType.AUDIO -> "Music"
                        null -> "Library"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("${visibleItems.size} items", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = repository::scanAll) {
                Icon(Icons.Default.Refresh, "Rescan")
            }
            IconButton(onClick = onSelectFolder) {
                Icon(Icons.Default.FolderOpen, "Add folder")
            }
        }

        if (repository.isScanning) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Scanning media…")
            }
        }

        repository.scanError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        }

        if (visibleItems.isEmpty() && !repository.isScanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No media found")
                    Spacer(Modifier.size(12.dp))
                    Button(onClick = onSelectFolder) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select a folder")
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(visibleItems, key = { it.uri.toString() }) { item ->
                    LibraryItemRow(item, onOpenItem)
                }
            }
        }
    }
}

@Composable
private fun LibraryItemRow(item: LibraryItem, onOpenItem: (LibraryItem) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpenItem(item) }.padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (item.type == LibraryType.VIDEO) Icons.Default.Movie else Icons.Default.AudioFile,
            null,
            Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, maxLines = 1)
            Text(formatLibrarySize(item.sizeBytes), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatLibrarySize(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return "%.1f %s".format(value, units[index])
}


@Composable
fun FolderScreen(
    repository: LibraryRepository,
    onSelectFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Folders", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${repository.selectedFolders.size} selected folders",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onSelectFolder) {
                Icon(Icons.Default.FolderOpen, "Add folder")
            }
        }

        if (repository.selectedFolders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No folders selected")
                    Spacer(Modifier.size(12.dp))
                    Button(onClick = onSelectFolder) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select a folder")
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(repository.selectedFolders, key = { it }) { folder ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOpen, null, Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            folderDisplayName(folder),
                            Modifier.weight(1f),
                            maxLines = 1
                        )
                        IconButton(onClick = { repository.removeFolder(folder) }) {
                            Icon(Icons.Default.Delete, "Remove folder")
                        }
                    }
                }
            }
        }
    }
}

private fun folderDisplayName(treeUri: String): String {
    return runCatching {
        val documentId = DocumentsContract.getTreeDocumentId(Uri.parse(treeUri))
        documentId.substringAfter(':', documentId)
    }.getOrDefault(treeUri)
}
