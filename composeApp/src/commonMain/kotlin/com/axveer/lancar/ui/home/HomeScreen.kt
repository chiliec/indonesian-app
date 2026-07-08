package com.axveer.lancar.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axveer.lancar.ui.AppModule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(appModule: AppModule, onOpenModule: (String) -> Unit) {
    val vm = remember { HomeViewModel(appModule) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    val state by vm.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Lancar") }) }) { pad ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
                items(state.modules, key = { it.id }) { row ->
                    ElevatedCard(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            .clickable { onOpenModule(row.id) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(row.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("${row.cardCount} words · ${row.masteryPct}% mastered",
                                style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { row.masteryPct / 100f },
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
