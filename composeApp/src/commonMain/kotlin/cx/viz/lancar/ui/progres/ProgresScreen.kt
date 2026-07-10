package cx.viz.lancar.ui.progres

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cx.viz.lancar.domain.BoxRow
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LocalAccentColor

@Composable
fun ProgresScreen(appModule: AppModule) {
    val vm = remember { ProgresViewModel(appModule) }
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
                item { SummaryCard(state) }
                item { ReviewDeckCard(state) }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text("Progres.", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Sejauh mana kamu — how far you've come.",
            style = MaterialTheme.typography.bodyLarge,
            color = LancarSecondaryText,
        )
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, LancarBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun SummaryCard(s: ProgresUiState) {
    Card {
        Text("${s.mastered} / ${s.total} · ${s.masteryPct}%", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("kata dikuasai — words mastered", style = MaterialTheme.typography.bodySmall, color = LancarSecondaryText)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { s.masteryPct / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
            color = LocalAccentColor.current,
            trackColor = LancarPanel,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatChip(value = s.accuracyPct?.let { "$it%" } ?: "—", label = "Akurasi", modifier = Modifier.weight(1f))
            StatChip(value = "${s.seen}", label = "Dilihat", modifier = Modifier.weight(1f))
            StatChip(value = "${s.reviewDeck}", label = "Dalam ulasan", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LancarPanel)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = LocalAccentColor.current)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = LancarSecondaryText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReviewDeckCard(s: ProgresUiState) {
    Card {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Kotak ulasan", style = MaterialTheme.typography.titleMedium)
            Text("${s.reviewDeck} kata", style = MaterialTheme.typography.bodySmall, color = LancarSecondaryText)
        }
        Spacer(Modifier.height(14.dp))

        if (s.reviewDeck == 0) {
            Text(
                "Belum ada kata untuk diulang — mulai berlatih di Beranda.",
                style = MaterialTheme.typography.bodyMedium,
                color = LancarSecondaryText,
            )
        } else {
            val maxCount = s.boxRows.maxOf { it.count }
            s.boxRows.forEach { row ->
                BoxRowView(row, maxCount)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "🔥 ${s.dueToday} jatuh tempo hari ini · ${s.dueThisWeek} minggu ini",
                style = MaterialTheme.typography.bodySmall,
                color = LancarSecondaryText,
            )
        }
    }
}

@Composable
private fun BoxRowView(row: BoxRow, maxCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Kotak ${row.box}",
            style = MaterialTheme.typography.bodySmall,
            color = LancarSecondaryText,
            modifier = Modifier.width(64.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(LancarPanel),
        ) {
            val frac = when {
                maxCount == 0 -> 0f
                row.count == 0 -> 0f
                else -> (row.count.toFloat() / maxCount).coerceAtLeast(0.06f)
            }
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(LocalAccentColor.current),
            )
        }
        Text(
            "${row.count}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp).padding(start = 8.dp),
        )
    }
}
