package com.musti.radio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
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
