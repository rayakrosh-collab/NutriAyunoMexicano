package com.example.nutriayunomx.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilAjustesDao {
    @Query("SELECT * FROM perfil_ajustes WHERE id = 1")
    fun getPerfilAjustes(): Flow<PerfilAjustes?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePerfilAjustes(perfil: PerfilAjustes)
}
