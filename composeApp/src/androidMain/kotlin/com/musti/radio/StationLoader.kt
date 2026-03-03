package com.musti.radio

import android.content.Context
import org.json.JSONArray

object StationLoader {
    private fun parseFallbacks(o: org.json.JSONObject): List<String> {
        val list = mutableListOf<String>()
        if (o.has("fallbackUrls")) {
            val arr = o.optJSONArray("fallbackUrls")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val u = arr.optString(i).trim()
                    if (u.startsWith("http://") || u.startsWith("https://")) list.add(u)
                }
            }
        }
        val csv = o.optString("fallbackUrlCsv", "")
        if (csv.isNotBlank()) {
            csv.split(",").map { it.trim() }
                .filter { it.startsWith("http://") || it.startsWith("https://") }
                .forEach { if (!list.contains(it)) list.add(it) }
        }
        return list
    }

    fun load(context: Context): List<Station> {
        return runCatching {
            val text = context.assets.open("stations.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Station(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            emoji = o.optString("emoji", "🎵"),
                            streamUrl = o.getString("streamUrl"),
                            category = o.optString("category", "Genel"),
                            region = o.optString("region", "Türkiye"),
                            bitrateKbps = o.optInt("bitrateKbps", 128),
                            fallbackUrls = parseFallbacks(o),
                        )
                    )
                }
            }
        }.getOrElse { defaultStations }
    }
}
