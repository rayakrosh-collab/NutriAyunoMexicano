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

    @Query("""
        SELECT r.id, r.alimentoId, r.fecha, r.cantidadPorciones, r.proteinaCalculadaG, r.momento, 
               a.nombre AS alimentoNombre, a.porcionDescripcion 
        FROM registro_comida r 
        INNER JOIN alimento a ON r.alimentoId = a.id 
        WHERE r.fecha = :fecha 
        ORDER BY r.id DESC
    """)
    fun getRegistrosConAlimentoPorFecha(fecha: String): Flow<List<RegistroComidaConAlimento>>

    @Query("""
        SELECT r.id, r.alimentoId, r.fecha, r.cantidadPorciones, r.proteinaCalculadaG, r.momento, 
               a.nombre AS alimentoNombre, a.porcionDescripcion 
        FROM registro_comida r 
        INNER JOIN alimento a ON r.alimentoId = a.id 
        ORDER BY r.fecha DESC, r.id DESC
    """)
    fun getTodosLosRegistrosConAlimento(): Flow<List<RegistroComidaConAlimento>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistro(registro: RegistroComida): Long

    @Query("DELETE FROM registro_comida WHERE id = :id")
    suspend fun deleteRegistroPorId(id: Long)

    @Query("SELECT SUM(proteinaCalculadaG) FROM registro_comida WHERE fecha = :fecha")
    fun getProteinaTotalPorFecha(fecha: String): Flow<Double?>

    @Query("""
        SELECT fecha, SUM(proteinaCalculadaG) AS totalProteina 
        FROM registro_comida 
        WHERE fecha >= :fechaInicio 
        GROUP BY fecha 
        ORDER BY fecha ASC
    """)
    fun getProteinaDiariaPorRango(fechaInicio: String): Flow<List<FechaProteina>>
}
