package com.musti.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager

@UnstableApi
class RadioPlaybackService : Service() {

    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        val player = RadioPlayerHolder.get(this)

        createChannel()
        mediaSession = MediaSession.Builder(this, player).build()

        notificationManager = PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: androidx.media3.common.Player): CharSequence = "RadyoNova"
                override fun createCurrentContentIntent(player: androidx.media3.common.Player) = null
                override fun getCurrentContentText(player: androidx.media3.common.Player): CharSequence = "Canlı yayın çalıyor"
                override fun getCurrentLargeIcon(player: androidx.media3.common.Player, callback: PlayerNotificationManager.BitmapCallback) = null
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                    if (ongoing) startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            .build()
            .apply {
                setUseFastForwardAction(false)
                setUseRewindAction(false)
                setUseNextAction(false)
                setUsePreviousAction(false)
                setPlayer(player)
            }

        startForeground(NOTIFICATION_ID, bootstrapNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val player = RadioPlayerHolder.get(this)
        val prefs = AppPrefs(this)

        when (intent?.action) {
            ACTION_PLAY -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (!url.isNullOrBlank()) {
                    player.setMediaItem(MediaItem.fromUri(url))
                    player.prepare()
                    player.playWhenReady = true
                    prefs.setWasPlaying(true)
                }
            }

            ACTION_TOGGLE -> {
                if (player.currentMediaItem == null) {
                    val url = prefs.getLastStreamUrl()
                    if (!url.isNullOrBlank()) {
                        player.setMediaItem(MediaItem.fromUri(url))
                        player.prepare()
                        player.playWhenReady = true
                        prefs.setWasPlaying(true)
                    }
                } else {
                    player.playWhenReady = !player.playWhenReady
                    prefs.setWasPlaying(player.playWhenReady)
                }
            }

            ACTION_STOP -> {
                player.stop()
                prefs.setWasPlaying(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        RadyoNovaWidgetProvider.refresh(this)
        return START_STICKY
    }

    override fun onDestroy() {
        notificationManager?.setPlayer(null)
        notificationManager = null
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Radio Playback", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun bootstrapNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("RadyoNova")
            .setContentText("Arka planda oynatma hazır")
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "radio_playback"
        const val NOTIFICATION_ID = 9911
        const val ACTION_PLAY = "com.musti.radio.action.PLAY"
        const val ACTION_TOGGLE = "com.musti.radio.action.TOGGLE"
        const val ACTION_STOP = "com.musti.radio.action.STOP"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FALLBACK_CSV = "extra_fallback_csv"
    }
}
