package com.nexplay.app.playback

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.nexplay.app.NexPlayApplication

@OptIn(UnstableApi::class)
class NexPlayPlaybackService :
    MediaSessionService() {
    private var mediaSession:
            MediaSession? = null

    private var sessionPlayer:
            NexPlaySessionPlayer? = null

    override fun onCreate() {
        super.onCreate()

        val coordinator =
            (application as
                    NexPlayApplication)
                .playbackCoordinator

        val player =
            NexPlaySessionPlayer(
                coordinator,
            )

        sessionPlayer = player

        val session =
            MediaSession.Builder(
                this,
                player,
            ).build()

        mediaSession = session

        addSession(
            session,
        )
    }

    override fun onGetSession(
        controllerInfo:
        MediaSession.ControllerInfo,
    ): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession
            ?.release()

        mediaSession = null

        sessionPlayer
            ?.release()

        sessionPlayer = null

        super.onDestroy()
    }
}