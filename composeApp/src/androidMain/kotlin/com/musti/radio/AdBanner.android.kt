package com.musti.radio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

@Composable
actual fun AdBanner(modifier: Modifier) {
    // Stability hotfix: ad SDK kaynaklı class-loading crash'lerini önlemek için
    // banner geçici olarak pasif.
    Box(modifier = modifier.height(0.dp))
}
