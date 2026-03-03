package com.musti.radio

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import android.view.WindowManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            runCatching {
                val dir = File(filesDir, "logs")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, "radionova-crash.log")
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                f.appendText("[$ts] ${e::class.java.simpleName}: ${e.message}\n")
            }
        }
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
