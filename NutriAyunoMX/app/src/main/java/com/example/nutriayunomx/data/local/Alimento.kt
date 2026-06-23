package com.example.nutriayunomx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "alimento")
data class Alimento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val categoria: String,
    val porcionDescripcion: String,
    val porcionGramos: Double? = null,
    val proteinaG: Double,
    val caloriasKcal: Double? = null,
    val carbohidratosG: Double? = null,
    val grasasG: Double? = null,
    val origen: String,
    val codigoBarras: String? = null
)
