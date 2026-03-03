package com.musti.radio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
actual fun AdBanner(modifier: Modifier) {
    val context = LocalContext.current
    var loaded by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(if (loaded) 50.dp else 0.dp),
        factory = {
            runCatching {
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-3940256099942544/6300978111" // test unit
                    adListener = object : AdListener() {
                        override fun onAdLoaded() { loaded = true }
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) { loaded = false }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }.getOrElse {
                loaded = false
                AdView(context).apply { visibility = android.view.View.GONE }
            }
        },
        update = {
            runCatching { if (!loaded) it.loadAd(AdRequest.Builder().build()) }
        }
    )
}
