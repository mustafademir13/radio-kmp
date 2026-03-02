package com.musti.radio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class AndroidRadioPlayer(context: Context) : RadioPlayer {
    private var statusListener: (String) -> Unit = {}

    private val player = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> statusListener("Bağlanıyor…")
                    Player.STATE_READY -> statusListener(if (playWhenReady) "Çalıyor" else "Hazır")
                    Player.STATE_ENDED -> statusListener("Yayın sona erdi")
                    Player.STATE_IDLE -> statusListener("Hazır")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                statusListener("Hata: yayın açılamadı")
            }
        })
    }

    override fun play(url: String) {
        statusListener("Bağlanıyor…")
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    override fun stop() {
        player.stop()
        statusListener("Durduruldu")
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun setStatusListener(listener: (String) -> Unit) {
        statusListener = listener
    }
}
