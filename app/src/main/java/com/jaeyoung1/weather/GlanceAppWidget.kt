package com.jaeyoung1.weather

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text


class GlanceAppWidget: GlanceAppWidget() {
    @Composable
    override fun Content() {

        Box( modifier = GlanceModifier.fillMaxSize().background(Color.White).
        clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center) {
            Text(text = "hello")
        }

    }


}