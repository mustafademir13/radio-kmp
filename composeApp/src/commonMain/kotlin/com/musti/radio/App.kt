package com.musti.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private val BgDark = Color(0xFF080A16)
private val CardDark = Color(0xFF151C35)
private val CardDark2 = Color(0xFF1B2342)
private val NeonPurple = Color(0xFF7C4DFF)
private val NeonCyan = Color(0xFF22D3EE)
private val TextMain = Color(0xFFEAF0FF)
private val TextMuted = Color(0xFF9CA8C7)

@Composable
fun App(
    player: RadioPlayer,
    initialStationId: String? = null,
    initialFavorites: Set<String> = emptySet(),
    onStationChanged: (String) -> Unit = {},
    onFavoritesChanged: (Set<String>) -> Unit = {},
) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2D1762), BgDark, Color(0xFF0C1228))
                )
            )
            .padding(14.dp)
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("RadyoNova", color = TextMain, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1B2548))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("LIVE • NEON MODE", color = NeonCyan, style = MaterialTheme.typography.labelLarge)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Now Playing", color = TextMuted)
                        Text("${selectedStation.emoji} ${selectedStation.name}", color = TextMain, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Durum: $status", color = NeonCyan)

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(NeonCyan, NeonPurple)))
                                    .clickable {
                                        if (isPlaying) {
                                            player.stop(); isPlaying = false
                                        } else {
                                            player.play(selectedStation.streamUrl); player.setVolume(volume); isPlaying = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isPlaying) "❚❚" else "▶", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                favorites = if (favorites.contains(selectedStation.id)) favorites - selectedStation.id else favorites + selectedStation.id
                                onFavoritesChanged(favorites)
                            }) {
                                Text(if (favorites.contains(selectedStation.id)) "★ Favoride" else "☆ Favoriye ekle")
                            }
                        }

                        Text("Ses", color = TextMuted)
                        Slider(value = volume, onValueChange = { volume = it; player.setVolume(it) }, valueRange = 0f..1f)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Kanal ara") },
                    singleLine = true,
                )
            }

            item {
                Text("Stations", color = TextMain, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            items(filteredStations) { station ->
                val selected = station == selectedStation
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark2),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) NeonCyan else Color(0xFF344061)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val star = if (favorites.contains(station.id)) "★ " else ""
                            Text("$star${station.emoji} ${station.name}", color = TextMain, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            Text("${if (selected) "LIVE" else "128kbps"}", color = TextMuted)
                        }
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) NeonPurple else Color(0xFF2D3756))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(if (selected) "Seçili" else "Aç", color = Color.White)
                        }
                    }
                }
            }

            item { Box(modifier = Modifier.height(10.dp)) }
        }
    }
}
