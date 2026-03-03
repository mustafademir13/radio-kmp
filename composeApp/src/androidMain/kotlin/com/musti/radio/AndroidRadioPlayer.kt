package com.musti.radio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidRadioPlayer(context: Context) : RadioPlayer {
    private val appContext = context.applicationContext
    private val player = RadioPlayerHolder.get(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var statusListener: (String) -> Unit = {}
    private var playlistUrls: List<String> = emptyList()
    private var currentUrlIndex = 0
    private var retryCount = 0
    private val maxRetryPerUrl = 2

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
                logError("playerError code=${error.errorCodeName} msg=${error.message}")
                val current = playlistUrls.getOrNull(currentUrlIndex)
                if (current != null && retryCount < maxRetryPerUrl) {
                    retryCount++
                    statusListener("Bağlantı hatası, tekrar deneniyor…")
                    mainHandler.postDelayed({ safePlay(current) }, 1200L)
                    return
                }

                val nextIndex = currentUrlIndex + 1
                if (nextIndex < playlistUrls.size) {
                    currentUrlIndex = nextIndex
                    retryCount = 0
                    statusListener("Yedek yayına geçiliyor…")
                    safePlay(playlistUrls[currentUrlIndex])
                } else {
                    statusListener("Hata: yayın açılamadı")
                }
            }
        })
    }

    override fun play(url: String, fallbackUrls: List<String>) {
        playlistUrls = listOf(url) + fallbackUrls.filter { it.isNotBlank() && it != url }
        currentUrlIndex = 0
        retryCount = 0
        safePlay(playlistUrls.first())
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
                    }.onFailure {
                        logError("safePlay(mainPost) ${it.message}")
                        statusListener("Hata: oynatma başlatılamadı")
                    }
                }
            }
            statusListener("Bağlanıyor…")
        }.onFailure {
            logError("safePlay ${it.message}")
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
            logError("stop ${it.message}")
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
        }.onFailure { logError("setVolume ${it.message}") }
    }

    override fun setStatusListener(listener: (String) -> Unit) {
        statusListener = listener
    }

    private fun logError(msg: String) {
        runCatching {
            val dir = File(appContext.filesDir, "logs")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "radionova-player.log")
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            file.appendText("[$ts] $msg\n")
        }
    }
}
