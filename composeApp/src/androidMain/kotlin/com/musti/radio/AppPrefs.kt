package com.musti.radio

import android.content.Context

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("radionova_prefs", Context.MODE_PRIVATE)

    fun getLastStationId(): String? = prefs.getString("last_station_id", null)

    fun setLastStationId(id: String) {
        prefs.edit().putString("last_station_id", id).apply()
    }

    fun getFavorites(): Set<String> = prefs.getStringSet("favorite_station_ids", emptySet()) ?: emptySet()

    fun setFavorites(ids: Set<String>) {
        prefs.edit().putStringSet("favorite_station_ids", ids).apply()
    }
}
