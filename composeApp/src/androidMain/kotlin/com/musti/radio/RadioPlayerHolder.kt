package com.musti.radio

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object RadioPlayerHolder {
    @Volatile
    private var player: ExoPlayer? = null

    fun get(context: Context): ExoPlayer {
        return player ?: synchronized(this) {
            player ?: ExoPlayer.Builder(context.applicationContext).build().also { player = it }
        }
    }

    fun release() {
        player?.release()
        player = null
    }
}
