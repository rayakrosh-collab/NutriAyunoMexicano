package com.example.nutriayunomx.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlimentoDao {
    @Query("SELECT * FROM alimento ORDER BY nombre ASC")
    fun getAllAlimentos(): Flow<List<Alimento>>

    @Query("SELECT * FROM alimento WHERE nombre LIKE '%' || :query || '%' ORDER BY nombre ASC")
    fun buscarAlimentos(query: String): Flow<List<Alimento>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlimento(alimento: Alimento): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlimentos(alimentos: List<Alimento>)

    @Query("SELECT COUNT(*) FROM alimento")
    suspend fun getAlimentosCount(): Int
}
