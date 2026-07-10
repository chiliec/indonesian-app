package cx.viz.lancar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.sp
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LocalAccentColor

@Composable
fun HomeScreen(appModule: AppModule, onOpenModule: (String) -> Unit) {
    val vm = remember { HomeViewModel(appModule) }
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
                item { Header(state.name) }
                items(state.modules, key = { it.id }) { row ->
                    ModuleCard(
                        title = row.title,
                        subtitle = "${row.cardCount} kata · ${row.masteryPct}% mastered",
                        progress = row.masteryPct / 100f,
                        onClick = { onOpenModule(row.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(name: String) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LANCAR",
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(9.dp).clip(CircleShape).background(LocalAccentColor.current))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (name.isBlank()) "Selamat datang." else "Halo, $name.",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Pilih pelajaran — pick a module to practice.",
            style = MaterialTheme.typography.bodyLarge,
            color = LancarSecondaryText,
        )
    }
}

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    progress: Float,
    onClick: () -> Unit,
) {
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
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
            color = LocalAccentColor.current,
            trackColor = LancarPanel,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}
