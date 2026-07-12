package cx.viz.lancar.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import cx.viz.lancar.ui.home.HomeScreen
import cx.viz.lancar.ui.kartu.KartuScreen
import cx.viz.lancar.ui.profile.ProfileScreen
import cx.viz.lancar.ui.progres.ProgresScreen
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarInk
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSurface

private enum class Tab(val icon: String, val label: String) {
    BERANDA("🏠", "Beranda"),
    KARTU("🃏", "Kartu"),
    PROGRES("📊", "Progres"),
    PROFIL("👤", "Profil"),
}

@Composable
fun MainScaffold(
    appModule: AppModule,
    onOpenModule: (String) -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenReview: () -> Unit,
    onReplayOnboarding: () -> Unit,
) {
    var tab by remember { mutableStateOf(Tab.BERANDA) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (tab) {
            Tab.BERANDA -> HomeScreen(appModule, onOpenModule, onOpenReview)
            Tab.KARTU -> KartuScreen(appModule, onOpenDeck)
            Tab.PROGRES -> ProgresScreen(appModule)
            Tab.PROFIL -> ProfileScreen(appModule, onReplayOnboarding)
        }

        // Floating pill tab bar
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(LancarSurface)
                .border(1.5.dp, LancarBorder, RoundedCornerShape(26.dp))
                .padding(8.dp),
        ) {
            Tab.entries.forEach { t ->
                val selected = t == tab
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) LancarPanel else Color.Transparent)
                        .clickable { tab = t }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(t.icon, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) LancarInk else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
