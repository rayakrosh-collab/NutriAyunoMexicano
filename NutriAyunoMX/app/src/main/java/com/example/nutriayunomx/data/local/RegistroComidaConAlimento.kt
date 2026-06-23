package com.example.nutriayunomx.data.local

data class RegistroComidaConAlimento(
    val id: Long,
    val alimentoId: Long,
    val fecha: String, // Formato: YYYY-MM-DD
    val cantidadPorciones: Double,
    val proteinaCalculadaG: Double,
    val momento: String, // Desayuno, Comida, Cena, Colación
    val alimentoNombre: String,
    val porcionDescripcion: String
)
