package com.musti.radio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class Station(
    val id: String,
    val name: String,
    val emoji: String,
    val streamUrl: String,
    val category: String = "Genel",
    val region: String = "Türkiye",
    val bitrateKbps: Int = 128,
    val fallbackUrls: List<String> = emptyList(),
    val healthScore: Int = 70,
)

enum class BottomTab(val label: String) {
    Browse("Tümü"),
    Favorites("Favori"),
    Recent("Geçmiş"),
    Stable50("Önerilen"),
}

enum class SortMode(val label: String) {
    Popular("Popüler"),
    Name("İsim"),
    Bitrate("Bitrate"),
}

val defaultStations = listOf(
    Station("bbc_world", "BBC World Service", "🌍", "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service", "News", "UK", 128),
    Station("virgin_uk", "Virgin Radio UK", "🎸", "https://radio.virginradio.co.uk/stream", "Pop", "UK", 128),
    Station("kexp", "KEXP 90.3", "🎧", "https://kexp.streamguys1.com/kexp160.aac", "Indie", "US", 160),
    Station("powerturk", "Power Türk", "⚡", "https://listen.powerapp.com.tr/powerturk/mpeg/icecast.audio", "Pop", "Türkiye", 128),
    Station("slowturk", "Slow Türk", "🌙", "https://radyo.duhnet.tv/slowturk", "Slow", "Türkiye", 128),
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
    onSetAlarm: (Int) -> Unit = {},
    onCancelAlarm: () -> Unit = {},
    stations: List<Station> = defaultStations,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var selectedStation by remember {
        mutableStateOf(stations.firstOrNull { it.id == initialStationId } ?: stations.first())
    }
    var favorites by remember { mutableStateOf(initialFavorites) }
    val recent = remember { mutableStateListOf<String>() }
    var volume by remember { mutableStateOf(0.8f) }
    var status by remember { mutableStateOf("Hazır") }
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(BottomTab.Browse) }
    var selectedCategory by remember { mutableStateOf("Tümü") }
    var sortMode by remember { mutableStateOf(SortMode.Popular) }

    DisposableEffect(player) {
        player.setStatusListener { status = it }
        onDispose { player.setStatusListener { } }
    }

    val activeStations = stations.filter { it.healthScore >= 40 }
    val stable50 = activeStations.sortedWith(compareByDescending<Station> { it.healthScore }.thenByDescending { it.bitrateKbps }.thenBy { it.name }).take(50)
    val baseList = when (selectedTab) {
        BottomTab.Browse -> activeStations
        BottomTab.Favorites -> stations.filter { favorites.contains(it.id) }
        BottomTab.Recent -> stations.filter { recent.contains(it.id) }
        BottomTab.Stable50 -> stable50
    }
    val categories = listOf("Tümü") + baseList.map { it.category }.filter { it.isNotBlank() }.distinct().take(8)
    val categoryList = if (selectedCategory == "Tümü") baseList else baseList.filter { it.category == selectedCategory }
    val searched = categoryList.filter { it.name.contains(query, ignoreCase = true) }
    val sleepRemaining = player.sleepTimerRemainingMinutes()
    val filteredStations = when (sortMode) {
        SortMode.Popular -> searched.sortedByDescending { it.bitrateKbps }
        SortMode.Name -> searched.sortedBy { it.name }
        SortMode.Bitrate -> searched.sortedByDescending { it.bitrateKbps }
    }

    LaunchedEffect(stations) {
        while (true) {
            val playing = player.isPlayingNow()
            if (isPlaying != playing) isPlaying = playing
            val current = player.currentUrl()
            if (!current.isNullOrBlank()) {
                val matched = stations.firstOrNull { it.streamUrl == current || it.fallbackUrls.contains(current) }
                if (matched != null && matched.id != selectedStation.id) {
                    selectedStation = matched
                }
            }
            delay(1000)
        }
    }

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF2D1762), BgDark, Color(0xFF0C1228))))
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text("RadyoNova", color = TextMain, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Now Playing", color = TextMuted)

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(NeonPurple, NeonCyan))),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(selectedStation.emoji)
                                }
                                Column {
                                    Text(selectedStation.name, color = TextMain, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${selectedStation.category} • ${selectedStation.region}", color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Durum: $status", color = NeonCyan, modifier = Modifier.alpha(pulseAlpha), maxLines = 1)
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(NeonCyan, NeonPurple)))
                                        .clickable {
                                            if (isPlaying) {
                                                player.stop(); isPlaying = false
                                            } else {
                                                player.play(selectedStation.streamUrl, selectedStation.fallbackUrls); player.setVolume(volume); isPlaying = true
                                                recent.remove(selectedStation.id)
                                                recent.add(0, selectedStation.id)
                                                if (recent.size > 10) recent.removeLast()
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(if (isPlaying) "❚❚" else "▶", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                                }
                            }

                            Button(onClick = {
                                favorites = if (favorites.contains(selectedStation.id)) favorites - selectedStation.id else favorites + selectedStation.id
                                onFavoritesChanged(favorites)
                            }) {
                                Text(if (favorites.contains(selectedStation.id)) "★ Favoride" else "☆ Favoriye ekle")
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { player.setSleepTimer(15) }, modifier = Modifier.weight(1f)) { Text("15 dk") }
                                Button(onClick = { player.setSleepTimer(30) }, modifier = Modifier.weight(1f)) { Text("30 dk") }
                                Button(onClick = { player.setSleepTimer(60) }, modifier = Modifier.weight(1f)) { Text("60 dk") }
                            }
                            Button(
                                onClick = { player.cancelSleepTimer() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Uyku zamanlayıcısını iptal et") }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { onSetAlarm(15) }, modifier = Modifier.weight(1f)) { Text("Alarm +15") }
                                Button(onClick = { onSetAlarm(30) }, modifier = Modifier.weight(1f)) { Text("Alarm +30") }
                                Button(onClick = { onCancelAlarm() }, modifier = Modifier.weight(1f)) { Text("Alarm İptal") }
                            }
                            Text(
                                if (sleepRemaining > 0) "Uyku zamanlayıcısı: ${sleepRemaining} dk kaldı" else "Uyku zamanlayıcısı kapalı",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )

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
                        supportingText = { Text("İstasyon adı yaz") },
                        singleLine = true,
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(categories) { cat ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedCategory == cat) NeonPurple else Color(0xFF2D3756))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            ) { Text(cat, color = Color.White, style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(SortMode.entries) { mode ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sortMode == mode) Color(0xFF1FA2B8) else Color(0xFF2D3756))
                                    .clickable { sortMode = mode }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            ) { Text(mode.label, color = Color.White, style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }

                items(filteredStations) { station ->
                    val selected = station == selectedStation
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardDark2),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) NeonCyan else Color(0xFF344061),
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStation = station
                                onStationChanged(station.id)
                                recent.remove(station.id)
                                recent.add(0, station.id)
                                if (recent.size > 10) recent.removeLast()
                                player.play(station.streamUrl, station.fallbackUrls)
                                player.setVolume(volume)
                                isPlaying = true
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.7f), NeonCyan.copy(alpha = 0.7f)))),
                                    contentAlignment = Alignment.Center,
                                ) { Text(station.emoji) }

                                Column {
                                    val star = if (favorites.contains(station.id)) "★ " else ""
                                    Text("$star${station.name}", color = TextMain, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${station.category} • ${station.region}", color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${station.bitrateKbps} kbps • Skor ${station.healthScore}", color = if (selected) NeonCyan else TextMuted)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) NeonPurple else Color(0xFF2D3756))
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                            ) {
                                Text(if (selected && isPlaying) "Çalıyor" else if (selected) "Seçili" else if (station.healthScore < 55) "Zayıf" else "Oynat", color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }
            }




            // Mini Player
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark2),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF344061)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedStation.name,
                            color = TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (isPlaying) "Çalıyor • ${selectedStation.category}" else "Duraklatıldı",
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    player.stop(); isPlaying = false
                                } else {
                                    player.play(selectedStation.streamUrl, selectedStation.fallbackUrls)
                                    player.setVolume(volume)
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D3756))
                        ) {
                            Text(
                                if (isPlaying) "❚❚" else "▶",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { player.stop(); isPlaying = false },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D3756))
                        ) {
                            Text(
                                "■",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            AdBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF171E38))
                    .border(1.dp, Color(0xFF303A60), RoundedCornerShape(18.dp))
                    .navigationBarsPadding()
                    .height(68.dp)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BottomTab.entries.forEach { tab ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == tab) NeonPurple else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(tab.label, color = if (selectedTab == tab) Color.White else TextMuted)
                    }
                }
            }
        }
    }
}
