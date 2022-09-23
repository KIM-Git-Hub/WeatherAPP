package com.jaeyoung1.weather

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.realm.Realm
import io.realm.kotlin.where


class WidgetProvider : AppWidgetProvider() {

    private lateinit var realm: Realm

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds?.forEach { appWidgetId ->
            val views: RemoteViews = addViews(context)
            appWidgetManager?.updateAppWidget(appWidgetId, views)

            
        }
    }

    private fun intentToMainActivity(context: Context?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE)
    }

    private fun addViews(context: Context?): RemoteViews {
        val views = RemoteViews(context?.packageName, R.layout.app_widget_provider)
        views.setOnClickPendingIntent(R.id.widgetBackground, intentToMainActivity(context))

        realm = Realm.getDefaultInstance()
        val realmResult = realm.where<RealmModel>().findAll()
        views.setTextViewText(R.id.currentAddressWidget, realmResult[0]?.text)
        views.setTextViewText(R.id.currentTempWidget, realmResult[1]?.text)

        when (realmResult[2]?.text) {
            "01d" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.clear_sky)
            "02d" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.few_clouds)
            "03d" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.clouds)
            "04d" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.clouds)
            "09d" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.rain)
            "10d" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.rain)
            "11d" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.thunder)
            "13d" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.snow)
            "50d" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.mist)
            "01n" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.clear_sky)
            "02n" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.few_clouds)
            "03n" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.clouds)
            "04n" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.clouds)
            "09n" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.rain)
            "10n" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.rain)
            "11n" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.thunder)
            "13n" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.snow)
            "50n" -> views.setImageViewResource(R.id.weatherIcon, R.drawable.mist)
        }

        views.setTextViewText(R.id.updateTime, realmResult[3]?.text)
        views.setTextViewText(R.id.dayOfWeekWidget1, realmResult[4]?.text)

        return views
    }


    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)


    }

}