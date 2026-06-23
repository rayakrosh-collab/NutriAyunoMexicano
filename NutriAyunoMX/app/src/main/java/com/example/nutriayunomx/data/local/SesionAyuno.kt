package com.example.nutriayunomx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sesion_ayuno")
data class SesionAyuno(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inicio: Long, // Epoch timestamp en milisegundos
    val fin: Long? = null, // Epoch timestamp en milisegundos (nulo si el ayuno está activo)
    val horasObjetivo: Int,
    val completada: Boolean = false
)
