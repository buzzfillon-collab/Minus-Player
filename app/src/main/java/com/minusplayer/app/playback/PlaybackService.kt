package com.minusplayer.app.playback

import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2_000,
                50_000,
                1_500,
                2_000
            )
            .setBackBuffer(
                10_000,
                true
            )
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(5_000)
            .setSeekForwardIncrementMs(5_000)
            .build()

        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
