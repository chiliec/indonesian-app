package cx.viz.lancar.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.viz.lancar.ui.AppModule
import cx.viz.lancar.ui.theme.LancarCream
import cx.viz.lancar.ui.theme.LancarGreen
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LocalAccentColor

@Composable
fun OnboardingScreen(appModule: AppModule, onDone: () -> Unit) {
    val vm = remember { OnboardingViewModel(appModule.settings) }
    val state by vm.state.collectAsState()
    val accent = LocalAccentColor.current

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 26.dp, end = 26.dp, top = 74.dp, bottom = 34.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LANCAR", style = MaterialTheme.typography.titleMedium, letterSpacing = 1.5.sp)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
        }

        Spacer(Modifier.height(44.dp))

        if (state.step == 0) {
            Text("Ngobrol\nsantai.", style = MaterialTheme.typography.displayLarge, lineHeight = 52.sp)
            Spacer(Modifier.height(18.dp))
            Text(
                "Bahasa Indonesia for real life — the warung, the ojek, the landlord, and everything in between.",
                style = MaterialTheme.typography.bodyLarge,
                color = LancarSecondaryText,
            )
            Spacer(Modifier.height(28.dp))
            Chip("Sama-sama!", LancarGreen, LancarCream)
            Spacer(Modifier.height(12.dp))
            Chip("Enak banget.", accent, LancarCream)
            Spacer(Modifier.weight(1f))
            PrimaryButton("Mulai — let's start", accent) { vm.next() }
        } else {
            Text("Siapa nama kamu?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("What should we call you? (optional)", style = MaterialTheme.typography.bodyMedium, color = LancarSecondaryText)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::onNameChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton("Selesai ✓", accent) { vm.finish(); onDone() }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = vm::back, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("← Kembali", color = LancarSecondaryText)
            }
        }
    }
}

@Composable
private fun Chip(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 18.dp, vertical = 10.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = fg) }
}

@Composable
private fun PrimaryButton(text: String, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = LancarCream),
    ) { Text(text, style = MaterialTheme.typography.titleMedium) }
}
