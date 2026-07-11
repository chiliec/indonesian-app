package cx.viz.lancar.ui.kartu

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.viz.lancar.domain.Card
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarCream
import cx.viz.lancar.ui.theme.LancarInk
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LocalAccentColor

@Composable
fun CardDeckScreen(appModule: AppModule, moduleId: String, onBack: () -> Unit) {
    val vm = remember(moduleId) { CardDeckViewModel(appModule, moduleId) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    val state by vm.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 24.dp, end = 24.dp, top = 60.dp, bottom = 28.dp),
    ) {
        if (state.loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = LocalAccentColor.current)
            }
            return@Column
        }

        val pager = rememberPagerState(pageCount = { state.cards.size })

        // ── header: close · counter · shuffle ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "✕",
                style = MaterialTheme.typography.titleLarge,
                color = LancarSecondaryText,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 14.dp),
            )
            Text(
                "${pager.currentPage + 1} / ${state.cards.size}",
                style = MaterialTheme.typography.labelMedium,
                color = LancarSecondaryText,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.shuffled) LocalAccentColor.current else LancarPanel)
                    .clickable { vm.toggleShuffle() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("🔀", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(20.dp))

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().weight(1f),
            pageSpacing = 16.dp,
        ) { page ->
            FlipCard(card = state.cards[page], onPlay = { vm.playAudio(it) }, onSpeak = { vm.speak(it) })
        }
    }
}

@Composable
private fun FlipCard(card: Card, onPlay: (String) -> Unit, onSpeak: (String) -> Unit) {
    // Reset to front whenever the card identity changes (page swipe).
    var flipped by remember(card.id) { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, LancarBorder, RoundedCornerShape(24.dp))
            .clickable { flipped = !flipped }
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = flipped, label = "flip") { showBack ->
            if (!showBack) CardFront(card, onPlay) else CardBack(card, onSpeak)
        }
    }
}

@Composable
private fun CardFront(card: Card, onPlay: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            card.indonesian,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        val audio = card.audio
        if (audio != null) {
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalAccentColor.current)
                    .clickable { onPlay(audio) }
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔉  Putar audio", style = MaterialTheme.typography.labelLarge, color = LancarCream)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "ketuk untuk membalik · tap to flip",
            style = MaterialTheme.typography.labelSmall,
            color = LancarSecondaryText,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun CardBack(card: Card, onSpeak: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            card.english,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        if (!card.note.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(
                card.note!!,
                style = MaterialTheme.typography.bodyMedium,
                color = LancarSecondaryText,
                textAlign = TextAlign.Center,
            )
        }
        card.sentences.firstOrNull()?.let { s ->
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LancarPanel)
                    .padding(16.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            s.text,
                            style = MaterialTheme.typography.titleSmall,
                            color = LancarInk,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "🔉",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .clickable { onSpeak(s.text) }
                                .padding(start = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(s.en, style = MaterialTheme.typography.bodySmall, color = LancarSecondaryText)
                }
            }
        }
    }
}
