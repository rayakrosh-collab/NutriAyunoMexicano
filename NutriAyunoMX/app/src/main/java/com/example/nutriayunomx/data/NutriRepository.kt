package com.example.nutriayunomx.data

import com.example.nutriayunomx.data.local.Alimento
import com.example.nutriayunomx.data.local.AlimentoDao
import com.example.nutriayunomx.data.local.AppDatabase
import com.example.nutriayunomx.data.local.PerfilAjustes
import com.example.nutriayunomx.data.local.PerfilAjustesDao
import com.example.nutriayunomx.data.local.RegistroComida
import com.example.nutriayunomx.data.local.RegistroComidaDao
import com.example.nutriayunomx.data.local.SesionAyuno
import com.example.nutriayunomx.data.local.SesionAyunoDao
import kotlinx.coroutines.flow.Flow

interface NutriRepository {
    // Alimentos
    fun getAllAlimentos(): Flow<List<Alimento>>
    fun buscarAlimentos(query: String): Flow<List<Alimento>>
    suspend fun insertAlimento(alimento: Alimento): Long
    suspend fun getAlimentosCount(): Int

    // Registros Comida
    fun getRegistrosPorFecha(fecha: String): Flow<List<RegistroComida>>
    suspend fun insertRegistro(registro: RegistroComida): Long
    suspend fun deleteRegistroPorId(id: Long)
    fun getProteinaTotalPorFecha(fecha: String): Flow<Double?>

    // Sesiones Ayuno
    fun getHistorialAyunos(): Flow<List<SesionAyuno>>
    fun getAyunoActivo(): Flow<SesionAyuno?>
    suspend fun insertSesion(sesion: SesionAyuno): Long
    suspend fun finalizarAyuno(id: Long, fin: Long, completada: Boolean)
    suspend fun deleteSesionPorId(id: Long)

    // Ajustes Perfil
    fun getPerfilAjustes(): Flow<PerfilAjustes?>
    suspend fun savePerfilAjustes(perfil: PerfilAjustes)
}

class DefaultNutriRepository(
    private val database: AppDatabase
) : NutriRepository {

    private val alimentoDao = database.alimentoDao()
    private val registroComidaDao = database.registroComidaDao()
    private val sesionAyunoDao = database.sesionAyunoDao()
    private val perfilAjustesDao = database.perfilAjustesDao()

    // Alimentos
    override fun getAllAlimentos(): Flow<List<Alimento>> = alimentoDao.getAllAlimentos()
    override fun buscarAlimentos(query: String): Flow<List<Alimento>> = alimentoDao.buscarAlimentos(query)
    override suspend fun insertAlimento(alimento: Alimento): Long = alimentoDao.insertAlimento(alimento)
    override suspend fun getAlimentosCount(): Int = alimentoDao.getAlimentosCount()

    // Registros Comida
    override fun getRegistrosPorFecha(fecha: String): Flow<List<RegistroComida>> =
        registroComidaDao.getRegistrosPorFecha(fecha)
    override suspend fun insertRegistro(registro: RegistroComida): Long =
        registroComidaDao.insertRegistro(registro)
    override suspend fun deleteRegistroPorId(id: Long) =
        registroComidaDao.deleteRegistroPorId(id)
    override fun getProteinaTotalPorFecha(fecha: String): Flow<Double?> =
        registroComidaDao.getProteinaTotalPorFecha(fecha)

    // Sesiones Ayuno
    override fun getHistorialAyunos(): Flow<List<SesionAyuno>> = sesionAyunoDao.getHistorialAyunos()
    override fun getAyunoActivo(): Flow<SesionAyuno?> = sesionAyunoDao.getAyunoActivo()
    override suspend fun insertSesion(sesion: SesionAyuno): Long = sesionAyunoDao.insertSesion(sesion)
    override suspend fun finalizarAyuno(id: Long, fin: Long, completada: Boolean) =
        sesionAyunoDao.finalizarAyuno(id, fin, completada)
    override suspend fun deleteSesionPorId(id: Long) = sesionAyunoDao.deleteSesionPorId(id)

    // Ajustes Perfil
    override fun getPerfilAjustes(): Flow<PerfilAjustes?> = perfilAjustesDao.getPerfilAjustes()
    override suspend fun savePerfilAjustes(perfil: PerfilAjustes) =
        perfilAjustesDao.savePerfilAjustes(perfil)
}
