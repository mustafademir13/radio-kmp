package com.musti.radio

import android.content.Context

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("radionova_prefs", Context.MODE_PRIVATE)

    fun getLastStationId(): String? = prefs.getString("last_station_id", null)
    fun setLastStationId(id: String) = prefs.edit().putString("last_station_id", id).apply()

    fun getFavorites(): Set<String> = prefs.getStringSet("favorite_station_ids", emptySet()) ?: emptySet()
    fun setFavorites(ids: Set<String>) = prefs.edit().putStringSet("favorite_station_ids", ids).apply()

    fun setLastStreamUrl(url: String) = prefs.edit().putString("last_stream_url", url).apply()
    fun getLastStreamUrl(): String? = prefs.getString("last_stream_url", null)

    fun setLastFallbackCsv(csv: String) = prefs.edit().putString("last_fallback_csv", csv).apply()
    fun getLastFallbackCsv(): String = prefs.getString("last_fallback_csv", "") ?: ""

    fun setWasPlaying(value: Boolean) = prefs.edit().putBoolean("was_playing", value).apply()
    fun wasPlaying(): Boolean = prefs.getBoolean("was_playing", false)

    fun setAlarmEpochMs(value: Long) = prefs.edit().putLong("alarm_epoch_ms", value).apply()
    fun getAlarmEpochMs(): Long = prefs.getLong("alarm_epoch_ms", 0L)
}
