package com.musti.radio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val player = AndroidRadioPlayer(this)
        val prefs = AppPrefs(this)

        setContent {
            App(
                player = player,
                initialStationId = prefs.getLastStationId(),
                initialFavorites = prefs.getFavorites(),
                onStationChanged = { prefs.setLastStationId(it) },
                onFavoritesChanged = { prefs.setFavorites(it) },
            )
        }
    }
}
