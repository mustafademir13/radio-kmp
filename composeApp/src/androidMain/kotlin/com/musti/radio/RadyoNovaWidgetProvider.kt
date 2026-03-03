package com.musti.radio

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class RadyoNovaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = AppPrefs(context)
        val wasPlaying = prefs.wasPlaying()
        val title = prefs.getLastStationId() ?: "RadyoNova"
        val sub = if (wasPlaying) "Çalıyor" else "Durdu"

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_radionova)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_sub, sub)

            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPi = PendingIntent.getActivity(
                context,
                100,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val toggleIntent = Intent(context, RadioPlaybackService::class.java).apply {
                action = RadioPlaybackService.ACTION_TOGGLE
            }
            val togglePi = PendingIntent.getService(
                context,
                101,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(context, RadioPlaybackService::class.java).apply {
                action = RadioPlaybackService.ACTION_STOP
            }
            val stopPi = PendingIntent.getService(
                context,
                102,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_title, launchPi)
            views.setOnClickPendingIntent(R.id.widget_sub, launchPi)
            views.setOnClickPendingIntent(R.id.widget_btn_toggle, togglePi)
            views.setOnClickPendingIntent(R.id.widget_btn_stop, stopPi)

            appWidgetManager.updateAppWidget(id, views)
        }
    }

    companion object {
        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, RadyoNovaWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val i = Intent(context, RadyoNovaWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(i)
            }
        }
    }
}
