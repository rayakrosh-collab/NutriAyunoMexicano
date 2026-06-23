package com.example.nutriayunomx.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SesionAyunoDao {
    @Query("SELECT * FROM sesion_ayuno ORDER BY inicio DESC")
    fun getHistorialAyunos(): Flow<List<SesionAyuno>>

    @Query("SELECT * FROM sesion_ayuno WHERE fin IS NULL LIMIT 1")
    fun getAyunoActivo(): Flow<SesionAyuno?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSesion(sesion: SesionAyuno): Long

    @Query("UPDATE sesion_ayuno SET fin = :fin, completada = :completada WHERE id = :id")
    suspend fun finalizarAyuno(id: Long, fin: Long, completada: Boolean)

    @Query("DELETE FROM sesion_ayuno WHERE id = :id")
    suspend fun deleteSesionPorId(id: Long)
}
