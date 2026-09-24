package com.minusplayer.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.minusplayer.app.playback.SubtitleSearchResult
import com.minusplayer.app.playback.SubtitleSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SubtitleSearchDialog(
    initialQuery: String,
    onDismiss: () -> Unit,
    onDownloaded: (Uri, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf(initialQuery) }
    var language by remember { mutableStateOf("en") }
    var languageExpanded by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SubtitleSearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var downloadingId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchRequest by remember { mutableIntStateOf(0) }

    val languages = listOf(
        "en" to "English", "bn" to "Bengali", "hi" to "Hindi", "ur" to "Urdu",
        "ar" to "Arabic", "es" to "Spanish", "fr" to "French", "de" to "German",
        "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese"
    )

    LaunchedEffect(searchRequest) {
        if (searchRequest == 0 || query.isBlank()) return@LaunchedEffect
        searching = true
        error = null
        results = emptyList()
        try {
            results = withContext(Dispatchers.IO) {
                SubtitleSearchService(context).search(query.trim(), language)
            }
        } catch (t: Throwable) {
            error = t.message ?: "Subtitle search failed."
        } finally {
            searching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search online subtitles") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Movie / episode") }
                )
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = !languageExpanded },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = languages.firstOrNull { it.first == language }?.second ?: language,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Language") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(languageExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    language = code
                                    languageExpanded = false
                                }
                            )
                        }
                    }
                }

                if (searching) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else if (error != null) {
                    Text(error!!, modifier = Modifier.padding(top = 12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(300.dp).padding(top = 8.dp)
                    ) {
                        items(results) { result ->
                            Button(
                                onClick = { downloadingId = result.fileId },
                                enabled = downloadingId == null,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Column {
                                    Text(result.fileName)
                                    Text(
                                        "${result.language} • ${result.downloads} downloads" +
                                            (result.release?.let { " • $it" } ?: "")
                                    )
                                }
                            }
                            if (downloadingId == result.fileId) {
                                LaunchedEffect(result.fileId) {
                                    try {
                                        val uri = withContext(Dispatchers.IO) {
                                            SubtitleSearchService(context).download(result)
                                        }
                                        onDownloaded(uri, result.language, result.fileName)
                                    } catch (t: Throwable) {
                                        error = t.message ?: "Subtitle download failed."
                                        downloadingId = null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { searchRequest++ },
                enabled = !searching && query.isNotBlank()
            ) {
                Text("Search")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
