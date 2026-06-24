package com.example.nutriayunomx.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.nutriayunomx.NutriAyunoApplication
import com.example.nutriayunomx.data.local.AppDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class FastingReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val db = AppDatabase.getInstance(context)
        val perfil = db.perfilAjustesDao().getPerfilAjustes().firstOrNull()

        if (perfil != null && perfil.recordatorioAyunoActivo) {
            sendReminderNotification()
            
            // Re-programar para el día siguiente (24 horas)
            val workRequest = OneTimeWorkRequestBuilder<FastingReminderWorker>()
                .setInitialDelay(24, TimeUnit.HOURS)
                .addTag("fasting_reminder_tag")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "fasting_reminder",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        return Result.success()
    }

    private fun sendReminderNotification() {
        val context = applicationContext
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val builder = NotificationCompat.Builder(context, NutriAyunoApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(com.example.nutriayunomx.R.string.notif_ayuno_titulo))
            .setContentText(context.getString(com.example.nutriayunomx.R.string.notif_ayuno_texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}
