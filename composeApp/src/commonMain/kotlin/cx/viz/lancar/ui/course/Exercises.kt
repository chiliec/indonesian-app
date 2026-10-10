package cx.viz.lancar.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cx.viz.lancar.domain.Step
import cx.viz.lancar.ui.drill.quiz.OptState
import cx.viz.lancar.ui.drill.quiz.OptionRow
import cx.viz.lancar.ui.theme.LancarBorder
import cx.viz.lancar.ui.theme.LancarInk
import cx.viz.lancar.ui.theme.LancarPanel
import cx.viz.lancar.ui.theme.LancarSecondaryText
import cx.viz.lancar.ui.theme.LancarSurface
import cx.viz.lancar.ui.theme.LocalAccentColor

/** Row colour for option i given selection and whether the step has been checked. */
fun optState(i: Int, selected: Int?, checked: Boolean, correctIndex: Int): OptState = when {
    !checked -> if (i == selected) OptState.SELECTED else OptState.IDLE
    i == correctIndex -> OptState.CORRECT
    i == selected -> OptState.WRONG
    else -> OptState.DIMMED
}

@Composable
private fun OptionList(options: List<String>, selected: Int?, checked: Boolean, correctIndex: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, label ->
            OptionRow(
                label = label,
                key = ('A' + i).toString(),
                state = optState(i, selected, checked, correctIndex),
                enabled = !checked,
                onClick = { onSelect(i) },
            )
        }
    }
}

@Composable
fun TheorySlide(step: Step.Theory, onSpeak: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(step.title, style = MaterialTheme.typography.headlineSmall, color = LancarInk)
        Text(step.body, style = MaterialTheme.typography.bodyLarge, color = LancarSecondaryText)
        step.examples.forEach { ex ->
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(LancarSurface, shape)
                    .border(1.5.dp, LancarBorder, shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(ex.id, style = MaterialTheme.typography.titleMedium, color = LancarInk)
                    Text(ex.en, style = MaterialTheme.typography.bodyMedium, color = LancarSecondaryText)
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .background(LancarPanel, CircleShape)
                        .clickable(onClickLabel = "Dengar · Listen") { onSpeak(ex.id) },
                    contentAlignment = Alignment.Center,
                ) { Text("🔊") }
            }
        }
    }
}

@Composable
fun ChooseTranslation(prompt: String, options: List<String>, selected: Int?, checked: Boolean, correctIndex: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(prompt, style = MaterialTheme.typography.headlineSmall, color = LancarInk)
        Spacer(Modifier.height(18.dp))
        OptionList(options, selected, checked, correctIndex, onSelect)
    }
}

@Composable
fun FillBlank(text: String, options: List<String>, selected: Int?, checked: Boolean, correctIndex: Int, onSelect: (Int) -> Unit) {
    val before = text.substringBefore("___")
    val after = text.substringAfter("___")
    val gap = selected?.let { options[it] } ?: "_____"
    val accent = LocalAccentColor.current
    Column {
        Text(
            buildAnnotatedString {
                append(before)
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { append(gap) }
                append(after)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = LancarInk,
        )
        Spacer(Modifier.height(18.dp))
        OptionList(options, selected, checked, correctIndex, onSelect)
    }
}

@Composable
fun TypeAnswer(prompt: String, text: String, checked: Boolean, onTextChange: (String) -> Unit, onDone: () -> Unit) {
    Column {
        Text(prompt, style = MaterialTheme.typography.headlineSmall, color = LancarInk)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            enabled = !checked,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ketik jawaban · Type your answer") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
    }
}
