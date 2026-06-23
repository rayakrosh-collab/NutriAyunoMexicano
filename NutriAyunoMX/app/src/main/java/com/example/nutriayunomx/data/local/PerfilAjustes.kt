package com.example.nutriayunomx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perfil_ajustes")
data class PerfilAjustes(
    @PrimaryKey
    val id: Int = 1, // Usamos ID fijo = 1 para asegurar que solo exista un registro de ajustes
    val pesoKg: Double? = null,
    val metaProteinaDiaria: Double = 80.0, // Default 80g
    val protocoloAyunoPreferido: String = "16:8",
    val unidades: String = "métrico"
)
