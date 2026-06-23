package com.example.nutriayunomx.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriayunomx.data.DefaultNutriRepository
import com.example.nutriayunomx.data.local.AppDatabase
import com.example.nutriayunomx.data.local.SesionAyuno
import com.example.nutriayunomx.data.local.Alimento
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    onItemClick: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(
            context = context,
            repository = DefaultNutriRepository(AppDatabase.getInstance(context))
        )
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Solicitar permiso de notificaciones (Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("⏱️", fontSize = 20.sp) },
                    label = { Text("Ayuno") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("🌮", fontSize = 20.sp) },
                    label = { Text("Alimentos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> FastingTabContent(viewModel = viewModel)
                1 -> FoodSearchTabContent(viewModel = viewModel)
                2 -> SettingsTabContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FastingTabContent(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val preferredProtocol by viewModel.preferredProtocol.collectAsStateWithLifecycle()
    val todayTotalProtein by viewModel.todayTotalProtein.collectAsStateWithLifecycle()
    val todayFoodLogs by viewModel.todayFoodLogs.collectAsStateWithLifecycle()
    val perfil by viewModel.perfilAjustes.collectAsStateWithLifecycle()

    val proteinGoal = perfil?.metaProteinaDiaria ?: 80.0
    val progress = if (proteinGoal > 0.0) (todayTotalProtein / proteinGoal).toFloat().coerceIn(0f, 1f) else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Título
        item {
            Text(
                text = "NutriAyuno MX ⏱️",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Temporizador o Selector
        item {
            if (activeSession != null) {
                ActiveTimerCard(
                    session = activeSession!!,
                    onStop = { viewModel.terminarAyuno() }
                )
            } else {
                StartFastingCard(
                    defaultProtocol = preferredProtocol,
                    onStart = { hours, isTesting ->
                        viewModel.iniciarAyuno(hours, isTesting)
                    }
                )
            }
        }

        // Panel de Proteína del Día (Fase 4)
        item {
            DailyProteinCard(
                todayTotalProtein = todayTotalProtein,
                proteinGoal = proteinGoal,
                progress = progress,
                todayFoodLogs = todayFoodLogs,
                onDeleteLog = { id -> viewModel.eliminarComida(id) }
            )
        }

        // Sección del Historial
        item {
            Text(
                text = "Historial de Ayunos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (history.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Aún no tienes ayunos registrados. ¡Comienza hoy!",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(history, key = { it.id }) { session ->
                HistoryItemCard(
                    session = session,
                    onDelete = { viewModel.eliminarAyuno(session.id) }
                )
            }
        }
    }
}

@Composable
fun DailyProteinCard(
    todayTotalProtein: Double,
    proteinGoal: Double,
    progress: Float,
    todayFoodLogs: List<com.example.nutriayunomx.data.local.RegistroComidaConAlimento>,
    onDeleteLog: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera con progreso numérico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Proteína de Hoy 🥚",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Progreso diario contra tu meta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", todayTotalProtein),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " / ${proteinGoal.toInt()}g",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}% completado",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (progress >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Barra de progreso lineal
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = if (progress >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Diario de comidas de hoy
            Text(
                text = "Diario de Alimentos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (todayFoodLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No has registrado alimentos hoy. Busca alimentos en la pestaña de al lado para registrar tu consumo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todayFoodLogs.forEach { log ->
                        FoodLogItemRow(log = log, onDelete = { onDeleteLog(log.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun FoodLogItemRow(
    log: com.example.nutriayunomx.data.local.RegistroComidaConAlimento,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.alimentoNombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Momento
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.momento,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }

                val portionText = if (log.cantidadPorciones == 1.0) "1 porción" else "${log.cantidadPorciones} porciones"
                Text(
                    text = "$portionText (${log.porcionDescripcion})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${log.proteinaCalculadaG}g",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Text("🗑️", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FoodSearchTabContent(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var selectedAlimento by remember { mutableStateOf<Alimento?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título y subtítulo
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Buscar Alimentos 🌮",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Consulta el contenido de proteína de comida mexicana y latina.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Buscador
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ej. frijoles, taco, huevo, pollo...") },
            leadingIcon = { Text("🔍", fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Text("❌", fontSize = 14.sp)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Resultados
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🍽️", fontSize = 48.sp)
                        Text(
                            text = "No se encontraron alimentos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Intenta buscar con otra palabra clave como 'taco', 'queso' o 'frijol'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults, key = { it.id }) { alimento ->
                    AlimentoItemCard(
                        alimento = alimento,
                        onClick = { selectedAlimento = alimento }
                    )
                }
            }
        }
    }

    // Diálogo de Detalle
    if (selectedAlimento != null) {
        AlimentoDetailDialog(
            alimento = selectedAlimento!!,
            onDismiss = { selectedAlimento = null },
            onRegister = { quantity, momento ->
                viewModel.registrarComida(
                    alimentoId = selectedAlimento!!.id,
                    cantidadPorciones = quantity,
                    proteinaPorPorcion = selectedAlimento!!.proteinaG,
                    momento = momento
                )
                selectedAlimento = null
            }
        )
    }
}

@Composable
fun SettingsTabContent(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val perfil by viewModel.perfilAjustes.collectAsStateWithLifecycle()
    
    var pesoInput by remember { mutableStateOf("") }
    var metaProteina by remember { mutableFloatStateOf(80f) }
    var selectedProtocol by remember { mutableStateOf("16:8") }
    var showSuccessToast by remember { mutableStateOf(false) }

    LaunchedEffect(perfil) {
        perfil?.let {
            pesoInput = it.pesoKg?.toString() ?: ""
            metaProteina = it.metaProteinaDiaria.toFloat()
            selectedProtocol = it.protocoloAyunoPreferido
        }
    }

    LaunchedEffect(showSuccessToast) {
        if (showSuccessToast) {
            delay(2000L)
            showSuccessToast = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Ajustes de Perfil ⚙️",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Personaliza tus metas diarias de proteína y tus preferencias de ayuno.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Éxito de guardado
        item {
            AnimatedVisibility(visible = showSuccessToast) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✅", fontSize = 18.sp)
                        Text(
                            text = "Ajustes guardados correctamente.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        // Peso Corporal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Peso Corporal (kg)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = pesoInput,
                        onValueChange = { pesoInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ej. 75") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Meta Proteína
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Meta de Proteína Diaria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${metaProteina.toInt()}g",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Fija tu objetivo de consumo de proteína diario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = metaProteina,
                        onValueChange = { metaProteina = it },
                        valueRange = 40f..200f,
                        steps = 160
                    )
                }
            }
        }

        // Protocolo Preferido
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Protocolo de Ayuno Preferido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("16:8", "18:6", "20:4").forEach { protocol ->
                            val isSelected = selectedProtocol == protocol
                            Button(
                                onClick = { selectedProtocol = protocol },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(protocol, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Botón Guardar
        item {
            Button(
                onClick = {
                    val pesoDouble = pesoInput.toDoubleOrNull()
                    viewModel.guardarAjustesPerfil(pesoDouble, metaProteina.toDouble(), selectedProtocol)
                    showSuccessToast = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("💾  Guardar Ajustes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AlimentoItemCard(
    alimento: Alimento,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = alimento.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (alimento.origen.equals("mexicano", ignoreCase = true) || alimento.origen.equals("latino", ignoreCase = true)) {
                        Text("🇲🇽", fontSize = 14.sp)
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge de categoría
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alimento.categoria,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = alimento.porcionDescripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "${alimento.proteinaG}g",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "proteína",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AlimentoDetailDialog(
    alimento: Alimento,
    onDismiss: () -> Unit,
    onRegister: (Double, String) -> Unit
) {
    var quantity by remember { mutableStateOf(1.0) }
    var selectedMomento by remember { mutableStateOf("Comida") }
    val momentos = listOf("Desayuno", "Comida", "Cena", "Colación")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onRegister(quantity, selectedMomento)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Registrar comida", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = alimento.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (alimento.origen.equals("mexicano", ignoreCase = true) || alimento.origen.equals("latino", ignoreCase = true)) {
                        Text("🇲🇽", fontSize = 18.sp)
                    }
                }
                Text(
                    text = alimento.categoria,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Porción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Porción sugerida:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = alimento.porcionDescripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Selección de Porciones
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cantidad de Porciones",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { if (quantity > 0.5) quantity -= 0.5 }) {
                            Text("➖", fontSize = 16.sp)
                        }
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f porciones", quantity),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { quantity += 0.5 }) {
                            Text("➕", fontSize = 16.sp)
                        }
                    }
                }

                // Selección de Momento de Comida
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Momento del Día",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        momentos.forEach { momento ->
                            val isSelected = selectedMomento == momento
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedMomento = momento }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = momento,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Grid de Macros calculados dinámicamente
                val factor = quantity
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Información Nutricional (calculado)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroCard(
                            label = "Proteína",
                            value = String.format(Locale.getDefault(), "%.1fg", alimento.proteinaG * factor),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        MacroCard(
                            label = "Calorías",
                            value = alimento.caloriasKcal?.let { String.format(Locale.getDefault(), "%d kcal", (it * factor).toInt()) } ?: "-",
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroCard(
                            label = "Carbohidratos",
                            value = alimento.carbohidratosG?.let { String.format(Locale.getDefault(), "%.1fg", it * factor) } ?: "-",
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        MacroCard(
                            label = "Grasas",
                            value = alimento.grasasG?.let { String.format(Locale.getDefault(), "%.1fg", it * factor) } ?: "-",
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun MacroCard(
    label: String,
    value: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}


@Composable
fun ActiveTimerCard(
    session: SesionAyuno,
    onStop: () -> Unit
) {
    var elapsedMillis by remember { mutableStateOf(System.currentTimeMillis() - session.inicio) }

    LaunchedEffect(session) {
        while (true) {
            elapsedMillis = System.currentTimeMillis() - session.inicio
            delay(1000L)
        }
    }

    val targetMillis = session.horasObjetivo * 3600 * 1000L
    // Si fue de prueba (detectado porque pasaron pocos minutos), usamos minutos para calcular el porcentaje
    // de lo contrario usamos horas estándar.
    val isTesting = elapsedMillis < targetMillis && (targetMillis > 48 * 3600 * 1000L || session.horasObjetivo <= 2) 
    
    val progress = if (isTesting) {
        // En test de 1 min, targetMillis representará 1 minuto en la UI
        val testTargetMillis = session.horasObjetivo * 60 * 1000L
        (elapsedMillis.toFloat() / testTargetMillis).coerceIn(0f, 1f)
    } else {
        (elapsedMillis.toFloat() / targetMillis).coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ayuno Activo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Canvas circular de progreso
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                val colorPrimary = MaterialTheme.colorScheme.primary
                val colorBackground = MaterialTheme.colorScheme.surfaceVariant
                
                Canvas(modifier = Modifier.size(160.dp)) {
                    // Círculo de fondo
                    drawCircle(
                        color = colorBackground,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 12.dp.toPx())
                    )
                    // Círculo de progreso
                    drawArc(
                        color = colorPrimary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatMillis(elapsedMillis),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Meta: ${session.horasObjetivo} hrs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Datos adicionales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Inicio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(session.inicio), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fin Estimado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val finEstimado = session.inicio + (session.horasObjetivo * 3600 * 1000L)
                    Text(formatTime(finEstimado), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Terminar Ayuno", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StartFastingCard(
    defaultProtocol: String,
    onStart: (Int, Boolean) -> Unit
) {
    var selectedHours by remember { mutableStateOf(16) }
    var isCustom by remember { mutableStateOf(false) }
    var isTestingMode by remember { mutableStateOf(false) }

    // Parsear el protocolo por defecto (ej: "16:8" -> 16)
    LaunchedEffect(defaultProtocol) {
        val parsedHours = defaultProtocol.split(":").firstOrNull()?.toIntOrNull()
        if (parsedHours != null) {
            selectedHours = parsedHours
            isCustom = parsedHours != 16 && parsedHours != 18 && parsedHours != 20
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Iniciar Nuevo Ayuno",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Opciones rápidas de protocolo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(16, 18, 20).forEach { hours ->
                    val isSelected = selectedHours == hours && !isCustom
                    Button(
                        onClick = {
                            selectedHours = hours
                            isCustom = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("${hours}:${24-hours}", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { isCustom = true },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isCustom) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Personalizado", fontWeight = FontWeight.Bold)
                }
            }

            // Slider personalizado
            AnimatedVisibility(visible = isCustom) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Horas de ayuno:", style = MaterialTheme.typography.bodyMedium)
                        Text("$selectedHours hrs", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = selectedHours.toFloat(),
                        onValueChange = { selectedHours = it.toInt() },
                        valueRange = 1f..48f,
                        steps = 47
                    )
                }
            }

            // Toggle para modo prueba
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Modo de prueba rápido", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("1 meta horas = 1 minuto para testear", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isTestingMode,
                    onCheckedChange = { isTestingMode = it }
                )
            }

            Button(
                onClick = { onStart(selectedHours, isTestingMode) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("▶️  Comenzar Ayuno", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    session: SesionAyuno,
    onDelete: () -> Unit
) {
    val durationMillis = if (session.fin != null) session.fin - session.inicio else 0L
    val hoursVal = durationMillis / (3600 * 1000.0)
    val minutesVal = durationMillis / (60 * 1000.0)

    // Formatear duración de forma legible
    val durationString = if (hoursVal >= 1.0) {
        String.format("%.1f horas", hoursVal)
    } else {
        String.format("%.1f minutos", minutesVal)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Ayuno de ${session.horasObjetivo} horas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatDate(session.inicio)} - ${if (session.fin != null) formatTime(session.fin) else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Duración: $durationString",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    // Badge de completado
                    val containerColor = if (session.completada) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    val contentColor = if (session.completada) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val label = if (session.completada) "Completado" else "Incompleto"
                    
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(containerColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Text(
                    text = "🗑️",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

// Helpers de formateo
fun formatMillis(millis: Long): String {
    val totalSec = millis / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hr = totalSec / 3600
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hr, min, sec)
}

fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
