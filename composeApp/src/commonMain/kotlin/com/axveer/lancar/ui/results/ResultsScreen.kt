package com.axveer.lancar.ui.results

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axveer.lancar.ui.Results

@Composable
fun ResultsScreen(r: Results, onAgain: () -> Unit, onHome: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Done!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("${r.correct} / ${r.total} correct")
            Text("${r.newlyMastered} new word(s) mastered")
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAgain, Modifier.fillMaxWidth()) { Text("Again") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onHome, Modifier.fillMaxWidth()) { Text("Home") }
        }
    }
}
