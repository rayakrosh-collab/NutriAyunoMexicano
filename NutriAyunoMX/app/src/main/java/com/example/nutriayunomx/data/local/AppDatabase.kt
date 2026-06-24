package com.example.nutriayunomx.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Database(
    entities = [Alimento::class, RegistroComida::class, SesionAyuno::class, PerfilAjustes::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alimentoDao(): AlimentoDao
    abstract fun registroComidaDao(): RegistroComidaDao
    abstract fun sesionAyunoDao(): SesionAyunoDao
    abstract fun perfilAjustesDao(): PerfilAjustesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriayuno_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context.applicationContext))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Cargar seed de alimentos desde assets
                    val jsonString = context.assets.open("alimentos_seed.json")
                        .bufferedReader()
                        .use { it.readText() }
                    
                    val jsonParser = Json { 
                        ignoreUnknownKeys = true 
                    }
                    val alimentos = jsonParser.decodeFromString<List<Alimento>>(jsonString)
                    
                    val database = getInstance(context)
                    // Insertar todos los alimentos en lote
                    database.alimentoDao().insertAlimentos(alimentos)
                    
                    // Insertar perfil de ajustes por defecto (meta 80g)
                    database.perfilAjustesDao().savePerfilAjustes(PerfilAjustes(id = 1))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
