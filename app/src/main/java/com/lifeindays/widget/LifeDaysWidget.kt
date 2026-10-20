package com.lifeindays.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId

class LifeDaysWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
        scheduleMidnightRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle
    ) {
        update(context, manager, id)
    }

    override fun onEnabled(context: Context) = scheduleMidnightRefresh(context)

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> refreshAll(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.lifeindays.widget.ACTION_REFRESH"
        private const val DEFAULT_W_DP = 250
        private const val DEFAULT_H_DP = 120

        fun hasWidgets(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, LifeDaysWidget::class.java)).isNotEmpty()

        /** Re-render every placed widget (after settings change, and at midnight). */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LifeDaysWidget::class.java))
            if (ids.isEmpty()) return
            ids.forEach { update(context, manager, it) }
            scheduleMidnightRefresh(context)
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val model = LifeStore.model(context)
            val (wPx, hPx) = targetSizePx(context, manager, id)

            val views = RemoteViews(context.packageName, R.layout.widget)
            views.setImageViewBitmap(R.id.image, WidgetRenderer.render(context, model, wPx, hPx))
            views.setContentDescription(R.id.image, WidgetRenderer.describe(model))
            // Tapping the widget opens its settings.
            views.setOnClickPendingIntent(
                R.id.image,
                PendingIntent.getActivity(
                    context, 0, Intent(context, SettingsActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            manager.updateAppWidget(id, views)
        }

        /** Widget size in pixels from the launcher's reported dp range, capped for RemoteViews. */
        private fun targetSizePx(context: Context, manager: AppWidgetManager, id: Int): Pair<Int, Int> {
            val o = manager.getAppWidgetOptions(id)
            val landscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val wDp = (if (landscape) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
                       else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH))
                .takeIf { it > 0 } ?: DEFAULT_W_DP
            val hDp = (if (landscape) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                       else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT))
                .takeIf { it > 0 } ?: DEFAULT_H_DP

            val dm = context.resources.displayMetrics
            var w = (wDp * dm.density).toInt().coerceAtLeast(1)
            var h = (hDp * dm.density).toInt().coerceAtLeast(1)

            // RemoteViews rejects bitmaps beyond a memory budget tied to screen size; stay well inside it.
            val maxPixels = dm.widthPixels.toLong() * dm.heightPixels
            while (w.toLong() * h > maxPixels) {
                w = w * 9 / 10
                h = h * 9 / 10
            }
            return w to h
        }

        private fun refreshIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 0,
                Intent(context, LifeDaysWidget::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        /** Inexact alarm just after the next local midnight, so the number ticks down daily. */
        private fun scheduleMidnightRefresh(context: Context) {
            val at = LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 2_000L
            context.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(AlarmManager.RTC, at, refreshIntent(context))
        }
    }
}
