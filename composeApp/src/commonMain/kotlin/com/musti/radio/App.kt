package com.musti.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Station(
    val id: String,
    val name: String,
    val emoji: String,
    val streamUrl: String,
)

val allStations = listOf(
    Station("bbc_world", "BBC World Service", "🌍", "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service"),
    Station("virgin_uk", "Virgin Radio UK", "🎸", "https://radio.virginradio.co.uk/stream"),
    Station("kexp", "KEXP 90.3", "🎧", "https://kexp.streamguys1.com/kexp160.aac"),
    Station("powerturk", "Power Türk", "⚡", "https://listen.powerapp.com.tr/powerturk/mpeg/icecast.audio"),
    Station("slowturk", "Slow Türk", "🌙", "https://radyo.duhnet.tv/slowturk"),
)

@Composable
fun App(
    player: RadioPlayer,
    initialStationId: String? = null,
    initialFavorites: Set<String> = emptySet(),
    onStationChanged: (String) -> Unit = {},
    onFavoritesChanged: (Set<String>) -> Unit = {},
) {
    MaterialTheme {
        var isPlaying by remember { mutableStateOf(false) }
        var selectedStation by remember {
            mutableStateOf(allStations.firstOrNull { it.id == initialStationId } ?: allStations.first())
        }
        var favorites by remember { mutableStateOf(initialFavorites) }
        var volume by remember { mutableStateOf(0.8f) }
        var status by remember { mutableStateOf("Hazır") }
        var query by remember { mutableStateOf("") }

        DisposableEffect(player) {
            player.setStatusListener { status = it }
            onDispose { player.setStatusListener { } }
        }

        val filteredStations = allStations.filter { it.name.contains(query, ignoreCase = true) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("RadyoNova", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Canlı radyo keyfi • modern arayüz", style = MaterialTheme.typography.bodyMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Şu an seçili kanal", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${selectedStation.emoji} ${selectedStation.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        "Durum: $status",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (isPlaying) {
                                player.stop()
                                isPlaying = false
                            } else {
                                player.play(selectedStation.streamUrl)
                                player.setVolume(volume)
                                isPlaying = true
                            }
                        }) { Text(if (isPlaying) "Durdur" else "Oynat") }

                        Button(onClick = {
                            favorites = if (favorites.contains(selectedStation.id)) favorites - selectedStation.id else favorites + selectedStation.id
                            onFavoritesChanged(favorites)
                        }) {
                            Text(if (favorites.contains(selectedStation.id)) "★ Favoriden çıkar" else "☆ Favoriye ekle")
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

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kanal ara") },
                singleLine = true,
            )

            Text("Kanallar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredStations) { station ->
                    val selected = station == selectedStation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            )
                            .clickable {
                                selectedStation = station
                                onStationChanged(station.id)
                                if (isPlaying) player.play(station.streamUrl)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val star = if (favorites.contains(station.id)) "★ " else ""
                            Text("$star${station.emoji} ${station.name}", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            if (selected) Text("Seçili")
                        }
                    }
                }
            }
        }
    }
}
