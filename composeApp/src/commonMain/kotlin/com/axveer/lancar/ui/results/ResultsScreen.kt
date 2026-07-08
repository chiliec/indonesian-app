package com.axveer.lancar.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.axveer.lancar.ui.Results
import com.axveer.lancar.ui.theme.LancarAmber
import com.axveer.lancar.ui.theme.LancarCream
import com.axveer.lancar.ui.theme.LancarInk
import com.axveer.lancar.ui.theme.LancarTerracotta

@Composable
fun ResultsScreen(r: Results, onAgain: () -> Unit, onHome: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(LancarInk).padding(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("🍛", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text("Mantap!", style = MaterialTheme.typography.displayMedium, color = LancarCream)
            Spacer(Modifier.height(8.dp))
            Text(
                "Pelajaran selesai — lesson complete.",
                style = MaterialTheme.typography.bodyLarge,
                color = LancarCream.copy(alpha = 0.85f),
            )

            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(value = "${r.correct}/${r.total}", label = "benar")
                StatChip(value = "+${r.newlyMastered}", label = "kata baru")
            }

            Spacer(Modifier.height(40.dp))
            PrimaryButton("Ulangi ↻", onClick = onAgain)
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Beranda", onClick = onHome)
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(
        Modifier
            .widthIn(min = 96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LancarCream.copy(alpha = 0.10f))
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = LancarAmber)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = LancarCream.copy(alpha = 0.75f))
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LancarTerracotta)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = LancarCream, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, LancarCream.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = LancarCream, textAlign = TextAlign.Center)
    }
}
