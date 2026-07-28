package cx.viz.lancar.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.theme.Accent
import cx.viz.lancar.ui.theme.LancarAmber
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarInk
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.tabScreenBottomPadding
import cx.viz.lancar.ui.theme.topContentPadding

@Composable
fun ProfileScreen(appModule: AppModule, onReplayOnboarding: () -> Unit) {
    val vm = remember { ProfileViewModel(appModule) }
    val state by vm.state.collectAsState()

    var editingName by remember { mutableStateOf(false) }
    var confirmingReset by remember { mutableStateOf(false) }

    val topPad = topContentPadding(16.dp)
    val bottomPad = tabScreenBottomPadding()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(PaddingValues(start = 22.dp, end = 22.dp, top = topPad, bottom = bottomPad)),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(68.dp).clip(CircleShape).background(LancarAmber),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (state.name.firstOrNull() ?: 'L').uppercaseChar().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = LancarInk,
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                state.name.ifBlank { "Selamat datang" },
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        // Settings card
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, LancarBorder, RoundedCornerShape(22.dp)),
        ) {
            SettingRow("✏️", "Nama", state.name.ifBlank { "—" }) { editingName = true }
            Divider()
            AccentRow(state.accent) { vm.setAccent(it) }
            Divider()
            SettingRow("🔁", "Ulangi perkenalan", "") { onReplayOnboarding() }
            Divider()
            SettingToggleRow("👂", "Selalu tampilkan teks soal dengar", state.showListenText) {
                vm.setShowListenText(it)
            }
            Divider()
            SettingToggleRow("🔊", "Putar audio otomatis", state.autoPlay) {
                vm.setAutoPlay(it)
            }
            Divider()
            SettingRow("🗑️", "Reset progres", "") { confirmingReset = true }
            Divider()
            SettingRow("ℹ️", "Tentang", "Lancar 1.0")
        }
        Text(
            "Konten & audio: Lancar. Offline vocabulary trainer.",
            style = MaterialTheme.typography.bodySmall,
            color = LancarSecondaryText,
        )
    }

    if (editingName) {
        var draft by remember { mutableStateOf(state.name) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            confirmButton = { TextButton(onClick = { vm.setName(draft); editingName = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("Batal") } },
            title = { Text("Nama kamu") },
            text = {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, singleLine = true)
            },
        )
    }

    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            confirmButton = {
                TextButton(onClick = { vm.resetProgress(); confirmingReset = false }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmingReset = false }) { Text("Batal") } },
            title = { Text("Reset progres?") },
            text = { Text("Semua mastery akan dihapus. Nama dan tema tetap. Tindakan ini tidak bisa dibatalkan.") },
        )
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LancarPanel))
}

@Composable
private fun SettingToggleRow(icon: String, label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun SettingRow(icon: String, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = LancarSecondaryText)
        }
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Text("›", color = LancarSecondaryText)
        }
    }
}

@Composable
private fun AccentRow(current: Accent, onPick: (Accent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🎨", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(14.dp))
        Text("Warna", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Accent.entries.forEach { a ->
            Box(
                Modifier
                    .padding(start = 10.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(a.color)
                    .border(
                        width = if (a == current) 3.dp else 0.dp,
                        color = if (a == current) LancarInk else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clickable { onPick(a) },
            )
        }
    }
}
