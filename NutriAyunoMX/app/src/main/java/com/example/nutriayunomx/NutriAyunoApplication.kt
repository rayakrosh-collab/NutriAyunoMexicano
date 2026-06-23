package com.example.nutriayunomx

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class NutriAyunoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones de Ayuno"
            val descriptionText = "Notificaciones de objetivos de ayuno completados"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "fasting_notifications_channel"
    }
}
