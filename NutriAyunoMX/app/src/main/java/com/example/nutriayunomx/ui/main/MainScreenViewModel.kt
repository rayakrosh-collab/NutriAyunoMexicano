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
import com.example.nutriayunomx.data.local.PerfilAjustes
import com.example.nutriayunomx.data.local.RegistroComida
import com.example.nutriayunomx.data.local.RegistroComidaConAlimento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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

    val perfilAjustes: StateFlow<PerfilAjustes?> = repository.getPerfilAjustes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayFoodLogs: StateFlow<List<RegistroComidaConAlimento>> = repository.getRegistrosConAlimentoPorFecha(getTodayDateString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotalProtein: StateFlow<Double> = repository.getProteinaTotalPorFecha(getTodayDateString())
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weeklyProteinData: StateFlow<List<com.example.nutriayunomx.data.local.FechaProteina>> = repository.getProteinaDiariaPorRango(getDaysAgoDateString(6))
        .map { dbList ->
            val map = dbList.associateBy { it.fecha }
            (0..6).map { i ->
                val dateStr = getDaysAgoDateString(6 - i)
                map[dateStr] ?: com.example.nutriayunomx.data.local.FechaProteina(dateStr, 0.0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyFastingData: StateFlow<List<Double>> = repository.getHistorialAyunos()
        .map { list ->
            val completedSessions = list.filter { it.completada }
            val map = completedSessions.groupBy {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(it.inicio))
            }
            (0..6).map { i ->
                val dateStr = getDaysAgoDateString(6 - i)
                val sessionsForDay = map[dateStr] ?: emptyList()
                sessionsForDay.sumOf { session ->
                    val finTime = session.fin ?: session.inicio
                    val durationMillis = finTime - session.inicio
                    durationMillis / (3600 * 1000.0)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0.0 })

    val fastingStreak: StateFlow<Int> = repository.getHistorialAyunos()
        .map { list -> calcularRacha(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Alimento>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllAlimentos()
            } else {
                repository.buscarAlimentos(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun registrarComida(alimentoId: Long, cantidadPorciones: Double, proteinaPorPorcion: Double, momento: String) {
        viewModelScope.launch {
            val proteinaCalculada = cantidadPorciones * proteinaPorPorcion
            val registro = RegistroComida(
                alimentoId = alimentoId,
                fecha = getTodayDateString(),
                cantidadPorciones = cantidadPorciones,
                proteinaCalculadaG = proteinaCalculada,
                momento = momento
            )
            repository.insertRegistro(registro)
        }
    }

    fun eliminarComida(id: Long) {
        viewModelScope.launch {
            repository.deleteRegistroPorId(id)
        }
    }

    fun guardarAjustesPerfil(peso: Double?, metaProteina: Double, protocolo: String) {
        viewModelScope.launch {
            val actual = PerfilAjustes(
                id = 1,
                pesoKg = peso,
                metaProteinaDiaria = metaProteina,
                protocoloAyunoPreferido = protocolo
            )
            repository.savePerfilAjustes(actual)
        }
    }



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

    private fun getDaysAgoDateString(daysAgo: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(cal.time)
    }

    private fun calcularRacha(history: List<SesionAyuno>): Int {
        val completedDates = history
            .filter { it.completada }
            .map { 
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(it.inicio))
            }
            .toSet()

        if (completedDates.isEmpty()) return 0

        val todayStr = getDaysAgoDateString(0)
        val yesterdayStr = getDaysAgoDateString(1)

        var currentStreak = 0
        var checkDate = if (completedDates.contains(todayStr)) todayStr else if (completedDates.contains(yesterdayStr)) yesterdayStr else null

        if (checkDate != null) {
            var daysAgo = if (checkDate == todayStr) 0 else 1
            while (completedDates.contains(getDaysAgoDateString(daysAgo))) {
                currentStreak++
                daysAgo++
            }
        }

        return currentStreak
    }
}
