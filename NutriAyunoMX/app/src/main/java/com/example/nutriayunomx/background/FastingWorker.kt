package com.example.nutriayunomx.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nutriayunomx.NutriAyunoApplication
import com.example.nutriayunomx.R

class FastingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val targetHours = inputData.getInt("target_hours", 16)
        
        sendCompletionNotification(targetHours)
        
        return Result.success()
    }

    private fun sendCompletionNotification(hours: Int) {
        val context = applicationContext
        
        // Verificar permiso en Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Si no se tiene permiso, no se puede lanzar la notificación directamente.
                // Sin embargo, Room persistirá el ayuno de todas formas.
                return
            }
        }

        val text = String.format(
            java.util.Locale("es", "MX"),
            context.getString(R.string.notif_ayuno_completo_texto),
            hours
        )
        val builder = NotificationCompat.Builder(context, NutriAyunoApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notif_ayuno_completo_titulo))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
