package com.musti.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Station(
    val name: String,
    val streamUrl: String,
)

private val stations = listOf(
    Station("BBC World Service", "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service"),
    Station("Virgin Radio UK", "https://radio.virginradio.co.uk/stream"),
    Station("KEXP 90.3", "https://kexp.streamguys1.com/kexp160.aac"),
    Station("Power Türk", "https://listen.powerapp.com.tr/powerturk/mpeg/icecast.audio"),
    Station("Slow Türk", "https://radyo.duhnet.tv/slowturk"),
)

@Composable
fun App(player: RadioPlayer) {
    MaterialTheme {
        var isPlaying by remember { mutableStateOf(false) }
        var selectedStation by remember { mutableStateOf(stations.first()) }
        var volume by remember { mutableStateOf(0.8f) }
        var status by remember { mutableStateOf("Hazır") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("RadyoNova", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Canlı radyo keyfi", style = MaterialTheme.typography.bodyMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Şu an seçili kanal", style = MaterialTheme.typography.labelMedium)
                    Text(selectedStation.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Durum: $status", style = MaterialTheme.typography.bodyMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (isPlaying) {
                                player.stop()
                                isPlaying = false
                                status = "Durduruldu"
                            } else {
                                player.play(selectedStation.streamUrl)
                                player.setVolume(volume)
                                isPlaying = true
                                status = "Çalıyor"
                            }
                        }) {
                            Text(if (isPlaying) "Durdur" else "Oynat")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ses", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            player.setVolume(it)
                        },
                        valueRange = 0f..1f,
                    )
                    Text("${(volume * 100).toInt()}%")
                }
            }

            Text("Kanallar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(stations) { station ->
                    val selected = station == selectedStation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStation = station
                                status = if (isPlaying) {
                                    player.play(station.streamUrl)
                                    "Çalıyor"
                                } else {
                                    "Hazır"
                                }
                            }
                            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(station.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            if (selected) Text("Seçili")
                        }
                    }
                }
            }
        }
    }
}
