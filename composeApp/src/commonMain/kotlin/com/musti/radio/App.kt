package com.musti.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App(
    player: RadioPlayer,
) {
    MaterialTheme {
        var isPlaying by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Mustafa Radio")
            Text("Kotlin Multiplatform başlangıç sürümü")

            Button(
                onClick = {
                    if (isPlaying) player.stop() else player.play("https://stream.live.vc.bbcmedia.co.uk/bbc_world_service")
                    isPlaying = !isPlaying
                },
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(if (isPlaying) "Durdur" else "Oynat")
            }
        }
    }
}
