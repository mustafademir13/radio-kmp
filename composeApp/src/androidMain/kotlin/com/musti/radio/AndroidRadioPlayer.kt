package com.musti.radio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.core.content.ContextCompat
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
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs = AppPrefs(appContext)

    private var statusListener: (String) -> Unit = {}
    private var playlistUrls: List<String> = emptyList()
    private var currentUrlIndex = 0
    private var retryCount = 0
    private val maxRetryPerUrl = 2
    private var sleepTimerDeadlineMs: Long = 0L
    private var sleepRunnable: Runnable? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                player.playWhenReady = false
                statusListener("Ses odağı kaybedildi")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player.volume = 0.3f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                player.volume = 1.0f
                if (playlistUrls.isNotEmpty()) {
                    player.playWhenReady = true
                    statusListener("Çalıyor")
                }
            }
        }
    }

    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
        } else null

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
        prefs.setLastStreamUrl(url)
        prefs.setLastFallbackCsv(fallbackUrls.joinToString(","))
        prefs.setWasPlaying(true)
        currentUrlIndex = 0
        retryCount = 0
        runCatching { ContextCompat.startForegroundService(appContext, Intent(appContext, RadioPlaybackService::class.java)) }
        if (!requestAudioFocus()) {
            statusListener("Ses odağı alınamadı")
            return
        }
        safePlay(playlistUrls.first())
        RadyoNovaWidgetProvider.refresh(appContext)
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
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
            abandonAudioFocus()
            runCatching { appContext.stopService(Intent(appContext, RadioPlaybackService::class.java)) }
            cancelSleepTimer()
            prefs.setWasPlaying(false)
            statusListener("Durduruldu")
            RadyoNovaWidgetProvider.refresh(appContext)
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

    override fun diagnosticsSummary(): String {
        val crash = tailLog(File(appContext.filesDir, "logs/radionova-crash.log"), 4)
        val playerLog = tailLog(File(appContext.filesDir, "logs/radionova-player.log"), 6)
        return buildString {
            append("Crash log (son 4):\n")
            append(if (crash.isBlank()) "- yok\n" else crash + "\n")
            append("\nPlayer log (son 6):\n")
            append(if (playerLog.isBlank()) "- yok" else playerLog)
        }
    }

    private fun tailLog(file: File, lines: Int): String {
        return runCatching {
            if (!file.exists()) return ""
            file.readLines().takeLast(lines).joinToString("\n")
        }.getOrDefault("")
    }


    override fun setSleepTimer(minutes: Int) {
        val safeMin = minutes.coerceAtLeast(1)
        cancelSleepTimer()
        sleepTimerDeadlineMs = System.currentTimeMillis() + safeMin * 60_000L
        val r = Runnable {
            stop()
            statusListener("Uyku zamanlayıcısı: oynatma durduruldu")
        }
        sleepRunnable = r
        mainHandler.postDelayed(r, safeMin * 60_000L)
        statusListener("Uyku zamanlayıcısı: ${safeMin} dk")
    }

    override fun cancelSleepTimer() {
        sleepRunnable?.let { mainHandler.removeCallbacks(it) }
        sleepRunnable = null
        sleepTimerDeadlineMs = 0L
    }

    override fun sleepTimerRemainingMinutes(): Int {
        if (sleepTimerDeadlineMs <= 0L) return 0
        val diff = sleepTimerDeadlineMs - System.currentTimeMillis()
        if (diff <= 0) return 0
        return ((diff + 59_999L) / 60_000L).toInt()
    }


    override fun isPlayingNow(): Boolean = player.isPlaying || player.playWhenReady

    override fun currentUrl(): String? = runCatching {
        player.currentMediaItem?.localConfiguration?.uri?.toString()
    }.getOrNull()

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
