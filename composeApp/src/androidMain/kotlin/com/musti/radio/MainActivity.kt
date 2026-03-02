package com.musti.radio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        val player = AndroidRadioPlayer(this)
        val prefs = AppPrefs(this)
        val stations = StationLoader.load(this)

        setContent {
            App(
                player = player,
                initialStationId = prefs.getLastStationId(),
                initialFavorites = prefs.getFavorites(),
                onStationChanged = { prefs.setLastStationId(it) },
                onFavoritesChanged = { prefs.setFavorites(it) },
                stations = stations,
            )
        }
    }
}
