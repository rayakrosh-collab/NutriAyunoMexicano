package com.example.nutriayunomx.data

import android.content.Context
import com.example.nutriayunomx.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface DataRepository {
  val data: Flow<List<String>>
}

class DefaultDataRepository(context: Context) : DataRepository {
  private val database = AppDatabase.getInstance(context)
  override val data: Flow<List<String>> = database.alimentoDao().getAllAlimentos().map { list ->
    list.map { alimento -> "${alimento.nombre} (${alimento.proteinaG}g prot / ${alimento.porcionDescripcion})" }
  }
}
