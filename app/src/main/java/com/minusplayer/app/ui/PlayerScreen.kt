package com.minusplayer.app.ui

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    player: Player?,
    modifier: Modifier = Modifier,
    onToggleFullscreen: () -> Unit = {}
) {
    var position by remember(player) { mutableLongStateOf(player?.currentPosition ?: 0L) }
    var duration by remember(player) { mutableLongStateOf(player?.duration ?: 0L) }
    var bufferedPosition by remember(player) { mutableLongStateOf(player?.bufferedPosition ?: 0L) }
    var isPlaying by remember(player) { mutableStateOf(player?.isPlaying == true) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var volume by remember(player) { mutableFloatStateOf(player?.volume ?: 1f) }

    val focusRequester = remember { FocusRequester() }

    fun wakeControls() {
        controlsVisible = true
        interactionTick++
    }

    DisposableEffect(player) {
        if (player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    position = player.currentPosition.coerceAtLeast(0L)
                    duration = player.duration.coerceAtLeast(0L)
                    bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)
                    isPlaying = player.isPlaying
                    volume = player.volume
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
    }

    LaunchedEffect(player) {
        while (player != null) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            volume = player.volume
            delay(250L)
        }
    }

    LaunchedEffect(player, isPlaying, interactionTick) {
        if (player != null && isPlaying) {
            delay(3000L)
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { wakeControls() },
                    onLongPress = {
                        menuExpanded = true
                        wakeControls()
                    }
                )
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || player == null) return@onPreviewKeyEvent false

                when (event.key) {
                    Key.Spacebar -> {
                        if (!event.isCtrlPressed && !event.isAltPressed) {
                            if (player.isPlaying) player.pause() else player.play()
                            wakeControls()
                            true
                        } else false
                    }
                    Key.DirectionLeft -> {
                        player.seekTo((player.currentPosition - 5000L).coerceAtLeast(0L))
                        wakeControls()
                        true
                    }
                    Key.DirectionRight -> {
                        player.seekTo(player.currentPosition + 5000L)
                        wakeControls()
                        true
                    }
                    Key.DirectionUp -> {
                        player.volume = (player.volume + 0.05f).coerceAtMost(1f)
                        volume = player.volume
                        wakeControls()
                        true
                    }
                    Key.DirectionDown -> {
                        player.volume = (player.volume - 0.05f).coerceAtLeast(0f)
                        volume = player.volume
                        wakeControls()
                        true
                    }
                    Key.Escape -> {
                        onToggleFullscreen()
                        wakeControls()
                        true
                    }
                    Key.M -> {
                        player.volume = if (player.volume == 0f) 1f else 0f
                        volume = player.volume
                        wakeControls()
                        true
                    }
                    Key.F -> {
                        onToggleFullscreen()
                        wakeControls()
                        true
                    }
                    Key.Menu -> {
                        menuExpanded = true
                        wakeControls()
                        true
                    }
                    else -> false
                }
            }
    ) {
        if (player == null) {
            Text(
                "Open a video to start playback",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                update = { view -> view.player = player }
            )
        }

        if (controlsVisible && player != null) {
            PlayerOverlayControls(
                player = player,
                position = position,
                duration = duration,
                bufferedPosition = bufferedPosition,
                volume = volume,
                onInteraction = ::wakeControls,
                onVolumeChanged = {
                    player.volume = it
                    volume = it
                },
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (player != null) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (isPlaying) "Pause" else "Play") },
                    leadingIcon = {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        if (isPlaying) player.pause() else player.play()
                        menuExpanded = false
                        wakeControls()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Speed 1.0×") },
                    onClick = {
                        player.setPlaybackSpeed(1f)
                        menuExpanded = false
                        wakeControls()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Speed 1.5×") },
                    onClick = {
                        player.setPlaybackSpeed(1.5f)
                        menuExpanded = false
                        wakeControls()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Speed 2.0×") },
                    onClick = {
                        player.setPlaybackSpeed(2f)
                        menuExpanded = false
                        wakeControls()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (volume == 0f) "Unmute" else "Mute") },
                    leadingIcon = {
                        Icon(
                            if (volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        player.volume = if (player.volume == 0f) 1f else 0f
                        volume = player.volume
                        menuExpanded = false
                        wakeControls()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Fullscreen") },
                    leadingIcon = {
                        Icon(Icons.Default.Fullscreen, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleFullscreen()
                        wakeControls()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayerOverlayControls(
    player: Player,
    position: Long,
    duration: Long,
    bufferedPosition: Long,
    volume: Float,
    onInteraction: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scrubPosition by remember(player) { mutableFloatStateOf(position.toFloat()) }
    var scrubbing by remember(player) { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = Color(0xE6111111),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatTime(if (scrubbing) scrubPosition.toLong() else position),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    if (duration > 0) {
                        LinearProgressIndicator(
                            progress = { (bufferedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Slider(
                            value = position.toFloat().coerceIn(0f, duration.toFloat()),
                            onValueChange = {
                                scrubbing = true
                                scrubPosition = it
                                onInteraction()
                            },
                            onValueChangeFinished = {
                                player.seekTo(scrubPosition.toLong())
                                scrubbing = false
                                onInteraction()
                            },
                            valueRange = 0f..duration.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    } else {
                        Slider(
                            value = 0f,
                            onValueChange = {},
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        )
                    }
                }

                Text(
                    formatTime(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    player.seekToPreviousMediaItem()
                    onInteraction()
                }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White)
                }
                IconButton(onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                    onInteraction()
                }) {
                    Icon(
                        if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    player.seekToNextMediaItem()
                    onInteraction()
                }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White)
                }

                Spacer(Modifier.weight(1f))

                IconButton(onClick = {
                    onVolumeChanged((volume - 0.1f).coerceAtLeast(0f))
                    onInteraction()
                }) {
                    Icon(Icons.Default.VolumeDown, "Volume down", tint = Color.White)
                }
                Text(
                    "\${(volume * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
                IconButton(onClick = {
                    onVolumeChanged((volume + 0.1f).coerceAtMost(1f))
                    onInteraction()
                }) {
                    Icon(Icons.Default.VolumeUp, "Volume up", tint = Color.White)
                }
                IconButton(onClick = {
                    onToggleFullscreen()
                    onInteraction()
                }) {
                    Icon(Icons.Default.Fullscreen, "Fullscreen", tint = Color.White)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3600L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
