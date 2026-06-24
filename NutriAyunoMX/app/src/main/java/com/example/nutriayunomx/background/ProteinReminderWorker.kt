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

class ProteinReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val db = AppDatabase.getInstance(context)
        val perfil = db.perfilAjustesDao().getPerfilAjustes().firstOrNull()

        if (perfil != null && perfil.recordatorioProteinaActivo) {
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("es", "MX")).format(java.util.Date())
            val todayProteinFlow = db.registroComidaDao().getProteinaTotalPorFecha(todayStr)
            val todayProtein = todayProteinFlow.firstOrNull() ?: 0.0
            
            if (todayProtein < perfil.metaProteinaDiaria) {
                sendReminderNotification(todayProtein, perfil.metaProteinaDiaria)
            }

            // Re-programar para el día siguiente (24 horas)
            val workRequest = OneTimeWorkRequestBuilder<ProteinReminderWorker>()
                .setInitialDelay(24, TimeUnit.HOURS)
                .addTag("protein_reminder_tag")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "protein_reminder",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        return Result.success()
    }

    private fun sendReminderNotification(current: Double, target: Double) {
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

        val text = String.format(
            java.util.Locale("es", "MX"),
            "Llevas %.1fg de tu meta de %.0fg. ¡Aún estás a tiempo de registrar tus alimentos!",
            current,
            target
        )

        val builder = NotificationCompat.Builder(context, NutriAyunoApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("¡Registra tu proteína de hoy! 🥚")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2002
    }
}
