package com.jaeyoung1.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
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
        return views
    }



}