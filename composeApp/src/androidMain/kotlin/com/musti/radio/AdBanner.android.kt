package com.musti.radio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
actual fun AdBanner(modifier: Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    var loaded by remember { mutableStateOf(false) }

    // Ekrana göre adaptive banner boyutu (BANNER yerine)
    val adWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }.toInt().coerceAtLeast(320)
    val adWidthDp = (adWidthPx / density.density).toInt().coerceAtLeast(320)
    val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = {
            runCatching {
                AdView(context).apply {
                    setAdSize(adaptiveSize)
                    adUnitId = "ca-app-pub-3940256099942544/6300978111" // test unit
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            loaded = true
                        }

                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            loaded = false
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }.getOrElse {
                loaded = false
                AdView(context).apply { visibility = android.view.View.GONE }
            }
        },
        update = {
            runCatching {
                if (!loaded) {
                    it.setAdSize(adaptiveSize)
                    it.loadAd(AdRequest.Builder().build())
                }
            }
        }
    )
}
