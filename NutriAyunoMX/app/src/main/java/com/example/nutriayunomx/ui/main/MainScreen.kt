package com.example.nutriayunomx.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val preferredProtocol by viewModel.preferredProtocol.collectAsStateWithLifecycle()

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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
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
