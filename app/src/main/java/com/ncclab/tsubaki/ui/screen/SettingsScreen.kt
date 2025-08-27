package com.ncclab.tsubaki.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncclab.tsubaki.data.model.EngineType
import com.ncclab.tsubaki.ui.viewmodel.ScannerViewModel

@Composable
fun SettingsScreen(viewModel: ScannerViewModel = hiltViewModel()) {
    var selectedEngine by remember { mutableStateOf(viewModel.getCurrentEngine()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Select Scanner Engine (Beta)", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        EngineType.values().forEach { engine ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = (engine == selectedEngine),
                    onClick = {
                        selectedEngine = engine
                        viewModel.setEngine(engine)
                    }
                )
                Text(
                    text = engine.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}