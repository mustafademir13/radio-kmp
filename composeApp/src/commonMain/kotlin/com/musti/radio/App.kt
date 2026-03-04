package com.musti.radio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "Sistem"),
    DARK("dark", "Karanlık"),
    LIGHT("light", "Aydınlık");

    companion object {
        fun fromKey(key: String): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class StationCategory(val label: String, val emoji: String) {
    POPULAR("Popüler", "🔥"), FAVORITES("Favoriler", "♥"), ALL("Tümü", "🎵"),
    POP("Pop", "🎤"), ROCK("Rock", "🎸"), FOLK("Türkü", "🪗"),
    ARABESK("Arabesk", "🌙"), NEWS("Haber", "📰"), CLASSICAL("Klasik", "🎻"),
    CHILL("Chill", "☁️"), SPORTS("Spor", "⚽"), OTHER("Diğer", "📻")
}

val defaultStations = listOf(
    Station("powerturk", "Power Türk", "⚡", "https://listen.powerapp.com.tr/powerturk/mpeg/icecast.audio", "Pop", "Türkiye", 128),
    Station("slowturk", "Slow Türk", "🌙", "https://radyo.duhnet.tv/slowturk", "Slow", "Türkiye", 128),
)

// SampleApp-like color system
private val BgDeep = Color(0xFF0D0D16)
private val BgCard = Color(0xFF181826)
private val BgCardAlt = Color(0xFF1E1E30)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentViolet = Color(0xFF7C3AED)
private val AccentCyan = Color(0xFF06B6D4)
private val AccentHeart = Color(0xFFEC4899)
private val AccentGold = Color(0xFFFBBF24)
private val AccentGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFF1F0FF)
private val TextSecondary = Color(0xFF9CA3B8)
private val TextMuted = Color(0xFF6B7280)
private val DividerColor = Color(0xFF2A2A40)

private val GradientMain = Brush.verticalGradient(listOf(Color(0xFF1A0533), BgDeep, BgDeep))
private val GradientCard = Brush.horizontalGradient(listOf(BgCard, BgCardAlt))
private val GradientActive = Brush.horizontalGradient(listOf(Color(0xFF2A1150), Color(0xFF1A1A2E)))
private val GradientNowPlaying = Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFF5B21B6), Color(0xFF6D28D9), Color(0xFF5B21B6)))

private fun Station.getCategory(): StationCategory {
    val t = category.lowercase(); val n = name.lowercase()
    return when {
        t.contains("haber") || n.contains("haber") -> StationCategory.NEWS
        t.contains("pop") || n.contains("pop") -> StationCategory.POP
        t.contains("rock") -> StationCategory.ROCK
        t.contains("türkü") || t.contains("folk") -> StationCategory.FOLK
        t.contains("arabesk") -> StationCategory.ARABESK
        t.contains("klasik") || t.contains("classical") -> StationCategory.CLASSICAL
        t.contains("sport") || n.contains("spor") -> StationCategory.SPORTS
        t.contains("chill") || t.contains("ambient") -> StationCategory.CHILL
        else -> StationCategory.OTHER
    }
}

private fun categoryColor(cat: StationCategory): Color = when (cat) {
    StationCategory.POPULAR -> AccentGold
    StationCategory.FAVORITES -> AccentHeart
    StationCategory.POP -> Color(0xFFEC4899)
    StationCategory.ROCK -> Color(0xFFF97316)
    StationCategory.FOLK -> Color(0xFF84CC16)
    StationCategory.ARABESK -> Color(0xFFA78BFA)
    StationCategory.NEWS -> Color(0xFF38BDF8)
    StationCategory.CLASSICAL -> AccentGold
    StationCategory.CHILL -> Color(0xFF67E8F9)
    StationCategory.SPORTS -> Color(0xFF4ADE80)
    else -> AccentPurple
}

@Composable
fun App(
    player: RadioPlayer,
    initialStationId: String? = null,
    initialFavorites: Set<String> = emptySet(),
    onStationChanged: (String) -> Unit = {},
    onFavoritesChanged: (Set<String>) -> Unit = {},
    isPro: Boolean = false,
    onSetPro: (Boolean) -> Unit = {},
    initialThemeMode: String = "system",
    onThemeModeChanged: (String) -> Unit = {},
    stations: List<Station> = defaultStations,
) {
    var currentlyPlaying by remember { mutableStateOf(stations.firstOrNull { it.id == initialStationId }) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(StationCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(initialFavorites) }
    var status by remember { mutableStateOf("Hazır") }
    var volume by remember { mutableStateOf(0.8f) }
    var hasUserSelectedStation by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(ThemeMode.fromKey(initialThemeMode)) }

    var showMenu by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val keepVisibleInFavorites = remember { mutableStateListOf<String>() }
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    DisposableEffect(player) {
        player.setStatusListener { status = it }
        onDispose { player.setStatusListener { } }
    }

    LaunchedEffect(stations) {
        while (true) {
            isPlaying = player.isPlayingNow()
            val current = player.currentUrl()
            if (!current.isNullOrBlank()) {
                stations.firstOrNull { it.streamUrl == current || it.fallbackUrls.contains(current) }?.let { currentlyPlaying = it }
            }
            delay(1000)
        }
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != StationCategory.FAVORITES) {
            keepVisibleInFavorites.clear()
        }
    }

    val filteredStations = remember(stations, selectedCategory, searchQuery, favorites, keepVisibleInFavorites) {
        stations
            .filter { s ->
                val categoryOk = when (selectedCategory) {
                    StationCategory.POPULAR -> s.healthScore >= 85 || s.bitrateKbps >= 192
                    StationCategory.FAVORITES -> favorites.contains(s.id) || keepVisibleInFavorites.contains(s.id)
                    StationCategory.ALL -> true
                    else -> s.getCategory() == selectedCategory
                }
                categoryOk && (searchQuery.isBlank() || s.name.contains(searchQuery, true) || s.category.contains(searchQuery, true))
            }
            .let { list ->
                when (selectedCategory) {
                    StationCategory.POPULAR -> list.sortedWith(compareByDescending<Station> { it.healthScore }.thenByDescending { it.bitrateKbps }.thenBy { it.name.lowercase() })
                    StationCategory.FAVORITES -> list.sortedBy { it.name.lowercase() }
                    StationCategory.ALL -> list.sortedBy { it.name.lowercase() }
                    else -> list.sortedWith(compareByDescending<Station> { it.healthScore }.thenBy { it.name.lowercase() })
                }
            }
    }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(1f, 1.15f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulseScale")
    val pulseAlpha by pulseAnim.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulseAlpha")

    val effectiveDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val BgDeep = if (effectiveDark) Color(0xFF0D0D16) else Color(0xFFF5F7FB)
    val BgCard = if (effectiveDark) Color(0xFF181826) else Color(0xFFFFFFFF)
    val BgCardAlt = if (effectiveDark) Color(0xFF1E1E30) else Color(0xFFF2F4FA)
    val TextPrimary = if (effectiveDark) Color(0xFFF1F0FF) else Color(0xFF0F172A)
    val TextSecondary = if (effectiveDark) Color(0xFF9CA3B8) else Color(0xFF475569)
    val TextMuted = if (effectiveDark) Color(0xFF6B7280) else Color(0xFF64748B)
    val DividerColor = if (effectiveDark) Color(0xFF2A2A40) else Color(0xFFD7DEEA)

    val GradientMain = if (effectiveDark) {
        Brush.verticalGradient(listOf(Color(0xFF1A0533), BgDeep, BgDeep))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEAF1FF), Color(0xFFF8FAFF), BgDeep))
    }
    val GradientCard = Brush.horizontalGradient(listOf(BgCard, BgCardAlt))
    val GradientActive = if (effectiveDark) {
        Brush.horizontalGradient(listOf(Color(0xFF2A1150), Color(0xFF1A1A2E)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFDDE8FF), Color(0xFFF1F5FF)))
    }
    val GradientNowPlaying = if (effectiveDark) {
        Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFF5B21B6), Color(0xFF6D28D9), Color(0xFF5B21B6)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF6D7CFF), Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFF6D7CFF)))
    }

    val showBottomPlayer = currentlyPlaying != null && (isPlaying || hasUserSelectedStation)
    val adSlotHeight = if (!isPro) 72.dp else 0.dp
    val playerSlotHeight = if (showBottomPlayer) 92.dp else 0.dp
    val listBottomPadding = 24.dp + adSlotHeight + playerSlotHeight

    Box(Modifier.fillMaxSize().background(GradientMain)) {
        Column(Modifier.fillMaxSize()) {
            // Header (SampleApp style)
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 4.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .clickable { showMenu = true }
                                    .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text("☰", color = Color.White, fontSize = 16.sp) }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("RadyoNova", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                                    Text("📻", fontSize = 18.sp, modifier = Modifier.padding(bottom = 4.dp))
                                }
                                Text("${stations.size} Türk Radyosu", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable { isSearching = !isSearching; if (!isSearching) searchQuery = "" }
                                .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(if (isSearching) "✕" else "🔍", fontSize = 16.sp, color = Color.White) }
                    }

                    AnimatedVisibility(visible = isSearching, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Radyo ara...", color = TextMuted, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = DividerColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = AccentPurple,
                                focusedContainerColor = BgCard,
                                unfocusedContainerColor = BgCard
                            )
                        )
                    }
                }
            }

            // Category chips
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StationCategory.entries) { cat ->
                    val selected = cat == selectedCategory
                    val chipColor = if (cat == StationCategory.ALL) AccentPurple else categoryColor(cat)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50.dp))
                            .background(if (selected) chipColor.copy(alpha = 0.25f) else BgCard)
                            .border(1.5.dp, if (selected) chipColor else DividerColor, RoundedCornerShape(50.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(cat.emoji, fontSize = 14.sp)
                            Text(cat.label, color = if (selected) chipColor else TextSecondary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }

            if (stations.isNotEmpty()) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                    Spacer(Modifier.width(6.dp))
                    Text("${filteredStations.size} istasyon", color = TextMuted, fontSize = 12.sp)
                }
            }

            // List above ad banner
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = listBottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredStations, key = { it.id }) { station ->
                    val isActive = currentlyPlaying?.id == station.id
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isActive) {
                                if (isPlaying) { player.stop(); isPlaying = false }
                                else { player.play(station.streamUrl, station.fallbackUrls); player.setVolume(volume); isPlaying = true }
                            } else {
                                currentlyPlaying = station
                                hasUserSelectedStation = true
                                onStationChanged(station.id)
                                player.play(station.streamUrl, station.fallbackUrls)
                                player.setVolume(volume)
                                isPlaying = true
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(90.dp).background(if (isActive) GradientActive else GradientCard)
                                .border(1.dp, if (isActive) AccentCyan.copy(alpha = 0.45f) else DividerColor, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 13.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                    Text(station.emoji, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(station.name.trim(), color = if (isActive) TextPrimary else TextPrimary.copy(alpha = 0.92f), fontSize = 16.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val catColor = categoryColor(station.getCategory())
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50.dp))
                                                .background(catColor.copy(alpha = 0.16f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("${station.getCategory().emoji} ${station.getCategory().label}", color = catColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text("• ${station.bitrateKbps}kbps", color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                                        .border(1.dp, if (favorites.contains(station.id)) AccentHeart.copy(alpha = 0.65f) else DividerColor, CircleShape)
                                        .clickable {
                                            val removing = favorites.contains(station.id)
                                            favorites = if (removing) favorites - station.id else favorites + station.id
                                            if (selectedCategory == StationCategory.FAVORITES && removing) {
                                                if (!keepVisibleInFavorites.contains(station.id)) keepVisibleInFavorites.add(station.id)
                                            } else {
                                                keepVisibleInFavorites.remove(station.id)
                                            }
                                            onFavoritesChanged(favorites)
                                        },
                                    contentAlignment = Alignment.Center
                                ) { Text(if (favorites.contains(station.id)) "♥" else "♡", color = if (favorites.contains(station.id)) AccentHeart else TextSecondary) }
                            }
                        }
                    }
                }
            }
        }

        // Bottom stack: player over ad, ad at very bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, BgDeep.copy(alpha = 0.85f), BgDeep)))
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            AnimatedVisibility(
                visible = showBottomPlayer,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut()
            ) {
                currentlyPlaying?.let { station ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = if (!isPro) 8.dp else 0.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(78.dp)
                                .background(GradientNowPlaying)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isPlaying) (40 * pulseScale).dp else 40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(station.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(station.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${station.category} • ${station.bitrateKbps}kbps", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .clickable {
                                        val removing = favorites.contains(station.id)
                                        favorites = if (removing) favorites - station.id else favorites + station.id
                                        if (selectedCategory == StationCategory.FAVORITES && removing) {
                                            if (!keepVisibleInFavorites.contains(station.id)) keepVisibleInFavorites.add(station.id)
                                        } else {
                                            keepVisibleInFavorites.remove(station.id)
                                        }
                                        onFavoritesChanged(favorites)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (favorites.contains(station.id)) "♥" else "♡", color = if (favorites.contains(station.id)) AccentHeart else Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .clickable {
                                        if (isPlaying) {
                                            player.stop(); isPlaying = false
                                        } else {
                                            player.play(station.streamUrl, station.fallbackUrls)
                                            player.setVolume(volume)
                                            isPlaying = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            if (!isPro) {
                AdBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                )
            }
        }

        // Full-screen menu
        AnimatedVisibility(showMenu) {
            Column(
                Modifier.fillMaxSize().background(Color(0xFF0F172A)).statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState())
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("RadyoNova Menü", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Ayarlar ve uygulama bilgileri", color = TextSecondary, fontSize = 12.sp)
                    }
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF1F2937)).clickable { showMenu = false }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Kapat", color = TextPrimary)
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(14.dp))

                MenuItem(if (isPro) "⭐ Pro Aktif" else "⭐ Pro'ya Geç") { onSetPro(!isPro) }

                Text("Tema", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = mode == themeMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) AccentPurple.copy(alpha = 0.25f) else Color(0xFF1F2937))
                                .border(1.dp, if (selected) AccentPurple else DividerColor, RoundedCornerShape(10.dp))
                                .clickable {
                                    themeMode = mode
                                    onThemeModeChanged(mode.key)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode.label, color = if (selected) TextPrimary else TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Text("Ses", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                Slider(
                    value = volume,
                    onValueChange = { volume = it; player.setVolume(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurple,
                        activeTrackColor = AccentPurple,
                        inactiveTrackColor = Color(0xFF374151)
                    )
                )

                MenuItem("ℹ️ Hakkında") { showAbout = true }
                MenuItem("📄 Kullanım Koşulları") { showTerms = true }
                MenuItem("🔒 Gizlilik") { showPrivacy = true }
                MenuItem("🧪 Tanı / Durum") { status = player.diagnosticsSummary().take(180) }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(10.dp))
                Text("RadyoNova • v1.4.0", color = TextMuted, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        if (showAbout) {
            AlertDialog(onDismissRequest = { showAbout = false }, title = { Text("Hakkında") },
                text = { Text("RadyoNova, Türk radyo istasyonlarını modern arayüzle dinlemen için geliştirildi.") },
                confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Tamam") } })
        }
        if (showTerms) {
            AlertDialog(onDismissRequest = { showTerms = false }, title = { Text("Kullanım Koşulları") },
                text = { Text("Yayınlar üçüncü taraf kaynaklardan gelir. İçerik hakları ilgili yayıncılara aittir.") },
                confirmButton = { TextButton(onClick = { showTerms = false }) { Text("Tamam") } })
        }
        if (showPrivacy) {
            AlertDialog(onDismissRequest = { showPrivacy = false }, title = { Text("Gizlilik") },
                text = { Text("Uygulama dinleme deneyimi için temel yerel tercihleri saklar. Ek kişisel veri toplamayı hedeflemez.") },
                confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Tamam") } })
        }
    }
}

@Composable
private fun MenuItem(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1F2937))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
