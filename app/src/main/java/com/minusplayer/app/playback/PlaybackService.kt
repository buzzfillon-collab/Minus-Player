package com.minusplayer.app.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.util.UnstableApi
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderManager
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private lateinit var decoderManager: DecoderManager
    private lateinit var historyStore: PlaybackHistoryStore
    private var fallbackAttemptedMediaId: String? = null

    override fun onCreate() {
        super.onCreate()
        historyStore = PlaybackHistoryStore(this)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2_000, 50_000, 1_500, 2_000)
            .setBackBuffer(10_000, true)
            .build()

        decoderManager = DecoderManager()
        val renderersFactory = NextRenderersFactory(this)
            .setDecoderManager(decoderManager)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(5_000)
            .setSeekForwardIncrementMs(5_000)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        decoderManager.attach(player!!)

        player!!.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (tryFfmpegFallback(error)) return
                player?.pause()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                saveResumePosition()
                fallbackAttemptedMediaId = null
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) saveResumePosition()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    saveResumePosition()
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    private fun tryFfmpegFallback(error: PlaybackException): Boolean {
        val currentPlayer = player ?: return false
        val mediaId = currentPlayer.currentMediaItem?.mediaId ?: return false
        if (fallbackAttemptedMediaId == mediaId) return false

        val decoderFailure = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED

        if (!decoderFailure) return false
        if (decoderManager.videoMode == DecoderMode.FFMPEG &&
            decoderManager.audioMode == DecoderMode.FFMPEG
        ) return false

        fallbackAttemptedMediaId = mediaId
        val position = currentPlayer.currentPosition.coerceAtLeast(0L)
        val shouldPlay = currentPlayer.playWhenReady
        decoderManager.selectVideoDecoder(DecoderMode.FFMPEG)
        decoderManager.selectAudioDecoder(DecoderMode.FFMPEG)
        currentPlayer.seekTo(position)
        currentPlayer.prepare()
        currentPlayer.playWhenReady = shouldPlay
        if (shouldPlay) currentPlayer.play()
        return true
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = player
        if (currentPlayer?.playWhenReady != true) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        saveResumePosition()
        decoderManager.detach()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun saveResumePosition() {
        val currentPlayer = player ?: return
        val mediaItem = currentPlayer.currentMediaItem ?: return
        val mediaId = mediaItem.mediaId
        val title = mediaItem.mediaMetadata.title?.toString().orEmpty()
        historyStore.record(
            mediaId = mediaId,
            title = title,
            positionMs = currentPlayer.currentPosition,
            durationMs = currentPlayer.duration
        )
    }
}
