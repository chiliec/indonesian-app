package cx.viz.lancar.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarCream
import cx.viz.lancar.ui.theme.LancarGreen
import cx.viz.lancar.ui.theme.LancarInk
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LancarSurface
import cx.viz.lancar.ui.theme.LocalAccentColor
import cx.viz.lancar.ui.theme.tabScreenBottomPadding
import cx.viz.lancar.ui.theme.topContentPadding

@Composable
fun KursusScreen(appModule: AppModule, onOpenLesson: (String) -> Unit) {
    // remember{} (not saveable): the tab recomposes fresh after a lesson pops, so the list refreshes itself.
    val vm = remember { KursusViewModel(appModule) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    val state by vm.state.collectAsState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state.loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = LocalAccentColor.current) }
            return@Box
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = topContentPadding(16.dp), bottom = tabScreenBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text("Kursus", style = MaterialTheme.typography.headlineMedium, color = LancarInk)
                    Text(
                        "${state.completedCount} dari ${state.totalCount} pelajaran",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LancarSecondaryText,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(state.rows, key = { it.id }) { row -> LessonCard(row) { onOpenLesson(row.id) } }
        }
    }
}

@Composable
private fun LessonCard(row: LessonRow, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val locked = row.state is RowState.Locked
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (locked) 0.45f else 1f)
            .background(LancarSurface, shape)
            .border(1.5.dp, LancarBorder, shape)
            .clickable(enabled = !locked, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).background(if (row.state is RowState.Done) LancarGreen else LancarPanel, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (row.state is RowState.Done) "✓" else "${row.index}",
                style = MaterialTheme.typography.labelLarge,
                color = if (row.state is RowState.Done) LancarCream else LancarInk,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.titleMedium, color = LancarInk)
            Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = LancarSecondaryText)
        }
        when (val s = row.state) {
            RowState.Current -> Text(
                "Mulai",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = LancarCream,
                modifier = Modifier.background(LocalAccentColor.current, RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            is RowState.Done -> Text("${s.score}/${s.total}", style = MaterialTheme.typography.labelMedium, color = LancarSecondaryText)
            RowState.Locked -> Text("🔒", style = MaterialTheme.typography.labelMedium)
        }
    }
}
