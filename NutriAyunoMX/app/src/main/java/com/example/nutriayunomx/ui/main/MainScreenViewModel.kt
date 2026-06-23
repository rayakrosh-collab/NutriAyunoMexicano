package com.example.nutriayunomx.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.nutriayunomx.background.FastingWorker
import com.example.nutriayunomx.data.NutriRepository
import com.example.nutriayunomx.data.local.Alimento
import com.example.nutriayunomx.data.local.SesionAyuno
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainScreenViewModel(
    private val context: Context,
    private val repository: NutriRepository
) : ViewModel() {

    val activeSession: StateFlow<SesionAyuno?> = repository.getAyunoActivo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val history: StateFlow<List<SesionAyuno>> = repository.getHistorialAyunos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferredProtocol: StateFlow<String> = repository.getPerfilAjustes()
        .map { it?.protocoloAyunoPreferido ?: "16:8" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "16:8")

    val foods: StateFlow<List<Alimento>> = repository.getAllAlimentos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun iniciarAyuno(horasObjetivo: Int, isTesting: Boolean = false) {
        viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            val session = SesionAyuno(
                inicio = inicio,
                horasObjetivo = horasObjetivo,
                completada = false
            )
            val id = repository.insertSesion(session)

            val delayTime = horasObjetivo.toLong()
            val timeUnit = if (isTesting) TimeUnit.MINUTES else TimeUnit.HOURS

            val workRequest = OneTimeWorkRequestBuilder<FastingWorker>()
                .setInputData(workDataOf("target_hours" to horasObjetivo))
                .setInitialDelay(delayTime, timeUnit)
                .addTag("fasting_worker")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "active_fast",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    fun terminarAyuno() {
        viewModelScope.launch {
            val active = repository.getAyunoActivo().firstOrNull()
            if (active != null) {
                val fin = System.currentTimeMillis()
                val elapsedMillis = fin - active.inicio
                val targetMillis = active.horasObjetivo * 3600 * 1000L
                
                val elapsedMinutes = elapsedMillis / (60 * 1000.0)
                val elapsedHours = elapsedMillis / (3600 * 1000.0)

                // Completado si pasó el tiempo objetivo en horas (o en minutos si fue de prueba)
                val completada = elapsedHours >= active.horasObjetivo || elapsedMinutes >= active.horasObjetivo

                repository.finalizarAyuno(active.id, fin, completada)

                WorkManager.getInstance(context).cancelUniqueWork("active_fast")
            }
        }
    }

    fun eliminarAyuno(id: Long) {
        viewModelScope.launch {
            repository.deleteSesionPorId(id)
        }
    }
}
