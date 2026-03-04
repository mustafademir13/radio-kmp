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

    fun setAlarmRepeatDaily(value: Boolean) = prefs.edit().putBoolean("alarm_repeat_daily", value).apply()
    fun isAlarmRepeatDaily(): Boolean = prefs.getBoolean("alarm_repeat_daily", false)

    fun setAlarmHour(value: Int) = prefs.edit().putInt("alarm_hour", value).apply()
    fun getAlarmHour(): Int = prefs.getInt("alarm_hour", 8)

    fun setAlarmMinute(value: Int) = prefs.edit().putInt("alarm_minute", value).apply()
    fun getAlarmMinute(): Int = prefs.getInt("alarm_minute", 0)

    fun setProEnabled(value: Boolean) = prefs.edit().putBoolean("pro_enabled", value).apply()
    fun isProEnabled(): Boolean = prefs.getBoolean("pro_enabled", false)

    fun setThemeMode(value: String) = prefs.edit().putString("theme_mode", value).apply()
    fun getThemeMode(): String = prefs.getString("theme_mode", "system") ?: "system"
}
