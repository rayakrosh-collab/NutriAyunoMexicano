package com.example.nutriayunomx.ui.main

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.nutriayunomx.data.DefaultDataRepository
import com.example.nutriayunomx.theme.NutriAyunoMXTheme

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository(context)) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  when (state) {
    MainScreenUiState.Loading -> {
      // Blank
    }
    is MainScreenUiState.Success -> {
      MainScreen(data = (state as MainScreenUiState.Success).data, modifier = modifier)
    }
    is MainScreenUiState.Error -> {
      Text("Error loading data: ${(state as MainScreenUiState.Error).throwable.message}")
    }
  }
}

@Composable
internal fun MainScreen(data: List<String>, modifier: Modifier = Modifier) {
  LazyColumn(modifier = modifier) {
    item {
      Text(text = "¡Alimentos Mexicanos cargados (${data.size})!")
    }
    items(data) { foodItem ->
      Text(text = foodItem)
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  NutriAyunoMXTheme { MainScreen(listOf("Android")) }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  NutriAyunoMXTheme { MainScreen(listOf("Android")) }
}
