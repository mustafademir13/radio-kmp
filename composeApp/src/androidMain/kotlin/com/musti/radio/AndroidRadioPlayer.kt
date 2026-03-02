package com.musti.radio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class AndroidRadioPlayer(context: Context) : RadioPlayer {
    private val player = ExoPlayer.Builder(context).build()

    override fun play(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    override fun stop() {
        player.stop()
    }
}
