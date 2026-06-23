package com.example.nutriayunomx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registro_comida")
data class RegistroComida(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alimentoId: Long,
    val fecha: String, // Formato: YYYY-MM-DD
    val cantidadPorciones: Double,
    val proteinaCalculadaG: Double,
    val momento: String // Desayuno, Comida, Cena, Colación
)
