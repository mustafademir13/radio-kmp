package com.musti.radio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = AppPrefs(context)
        if (!prefs.wasPlaying()) return
        val url = prefs.getLastStreamUrl() ?: return
        val fallbackCsv = prefs.getLastFallbackCsv()
        val playIntent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_PLAY
            putExtra(RadioPlaybackService.EXTRA_URL, url)
            putExtra(RadioPlaybackService.EXTRA_FALLBACK_CSV, fallbackCsv)
        }
        ContextCompat.startForegroundService(context, playIntent)
    }
}
