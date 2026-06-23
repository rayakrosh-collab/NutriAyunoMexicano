package com.example.nutriayunomx.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroComidaDao {
    @Query("SELECT * FROM registro_comida WHERE fecha = :fecha ORDER BY id DESC")
    fun getRegistrosPorFecha(fecha: String): Flow<List<RegistroComida>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistro(registro: RegistroComida): Long

    @Query("DELETE FROM registro_comida WHERE id = :id")
    suspend fun deleteRegistroPorId(id: Long)

    @Query("SELECT SUM(proteinaCalculadaG) FROM registro_comida WHERE fecha = :fecha")
    fun getProteinaTotalPorFecha(fecha: String): Flow<Double?>
}
