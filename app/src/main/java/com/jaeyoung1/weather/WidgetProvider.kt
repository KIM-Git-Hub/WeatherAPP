package com.jaeyoung1.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews


class WidgetProvider : AppWidgetProvider(){

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds?.forEach {
            appWidgetId ->
            val views: RemoteViews = addViews(context)
            appWidgetManager?.updateAppWidget(appWidgetId, views)
        }
    }

    private fun intentToMainActivity(context: Context?): PendingIntent{
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE)
    }

    private fun addViews(context: Context?): RemoteViews {
        val views = RemoteViews(context?.packageName, R.layout.widget)
        views.setTextViewText(R.id.widgetText, "hihi")
        views.setOnClickPendingIntent(R.id.widgetBackground, intentToMainActivity(context))
        return views
    }

}