package com.musti.radio

import android.content.Context
import org.json.JSONArray

object StationLoader {
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
                        )
                    )
                }
            }
        }.getOrElse { defaultStations }
    }
}
