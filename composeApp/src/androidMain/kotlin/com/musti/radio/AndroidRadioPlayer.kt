package com.musti.radio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

class AndroidRadioPlayer(private val context: Context) : RadioPlayer {
    private val appContext = context.applicationContext
    private val player = RadioPlayerHolder.get(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var statusListener: (String) -> Unit = {}
    private var lastUrl: String? = null
    private var retryCount = 0
    private val maxRetry = 3

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> statusListener("Bağlanıyor…")
                    Player.STATE_READY -> {
                        retryCount = 0
                        statusListener(if (player.playWhenReady) "Çalıyor" else "Hazır")
                    }
                    Player.STATE_ENDED -> statusListener("Yayın sona erdi")
                    Player.STATE_IDLE -> statusListener("Hazır")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (retryCount < maxRetry && !lastUrl.isNullOrBlank()) {
                    retryCount++
                    statusListener("Bağlantı koptu, tekrar deneniyor ($retryCount/$maxRetry)…")
                    mainHandler.postDelayed({
                        lastUrl?.let { play(it) }
                    }, 1500L * retryCount)
                } else {
                    statusListener("Hata: yayın açılamadı")
                }
            }
        })
    }

    override fun play(url: String) {
        lastUrl = url
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
