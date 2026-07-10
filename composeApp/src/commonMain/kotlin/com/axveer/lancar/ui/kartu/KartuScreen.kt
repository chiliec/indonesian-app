package com.axveer.lancar.ui.kartu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.axveer.lancar.ui.AppModule
import com.axveer.lancar.ui.theme.LancarBorder
import com.axveer.lancar.ui.theme.LancarSecondaryText
import com.axveer.lancar.ui.theme.LocalAccentColor

@Composable
fun KartuScreen(appModule: AppModule, onOpenDeck: (String) -> Unit) {
    val vm = remember { KartuViewModel(appModule) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    val state by vm.state.collectAsState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state.loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = LocalAccentColor.current)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 64.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Header() }
                items(state.modules, key = { it.id }) { row ->
                    DeckCard(
                        title = row.title,
                        subtitle = "${row.cardCount} kata",
                        onClick = { onOpenDeck(row.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text("Kartu.", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Balik kartunya — tap a module to flip through the words.",
            style = MaterialTheme.typography.bodyLarge,
            color = LancarSecondaryText,
        )
    }
}

@Composable
private fun DeckCard(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, LancarBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LancarSecondaryText)
    }
}
