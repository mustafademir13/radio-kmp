package com.musti.radio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

class AndroidRadioPlayer(context: Context) : RadioPlayer {
    private val appContext = context.applicationContext
    private val player = RadioPlayerHolder.get(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var statusListener: (String) -> Unit = {}
    private var lastUrl: String? = null
    private var retryCount = 0
    private val maxRetry = 2

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
                val url = lastUrl
                if (retryCount < maxRetry && !url.isNullOrBlank()) {
                    retryCount++
                    statusListener("Bağlantı hatası, tekrar deneniyor…")
                    mainHandler.postDelayed({ safePlay(url) }, 1200L)
                } else {
                    statusListener("Hata: yayın açılamadı")
                }
            }
        })
    }

    override fun play(url: String) {
        lastUrl = url
        safePlay(url)
    }

    private fun safePlay(url: String) {
        runCatching {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                player.playWhenReady = true
            } else {
                mainHandler.post {
                    runCatching {
                        player.setMediaItem(MediaItem.fromUri(url))
                        player.prepare()
                        player.playWhenReady = true
                    }.onFailure { statusListener("Hata: oynatma başlatılamadı") }
                }
            }
            statusListener("Bağlanıyor…")
        }.onFailure {
            statusListener("Hata: oynatma başlatılamadı")
        }
    }

    override fun stop() {
        runCatching {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                player.stop()
            } else {
                mainHandler.post { player.stop() }
            }
            statusListener("Durduruldu")
        }.onFailure {
            statusListener("Hata: durdurulamadı")
        }
    }

    override fun setVolume(volume: Float) {
        runCatching {
            val v = volume.coerceIn(0f, 1f)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                player.volume = v
            } else {
                mainHandler.post { player.volume = v }
            }
        }
    }

    override fun setStatusListener(listener: (String) -> Unit) {
        statusListener = listener
    }
}
