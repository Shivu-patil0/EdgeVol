package app.edgevol.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.edgevol.R
import app.edgevol.data.HandleSide
import app.edgevol.data.HandleSize
import app.edgevol.data.Prefs
import kotlin.math.roundToInt

/** Side, height, size and opacity of the handle. Every change is applied live to the real handle. */
@Composable
internal fun HandleSettingsCard(
    prefs: Prefs,
    onPrefsChange: ((Prefs) -> Prefs) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_handle_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )

            Text(stringResource(R.string.setup_handle_side), style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = listOf(
                    HandleSide.LEFT to stringResource(R.string.setup_handle_side_left),
                    HandleSide.RIGHT to stringResource(R.string.setup_handle_side_right),
                ),
                selected = prefs.side,
                onSelect = { side -> onPrefsChange { it.copy(side = side) } },
            )

            PercentSlider(
                stored = prefs.positionPct,
                range = Prefs.POSITION_MIN_PCT..Prefs.POSITION_MAX_PCT,
                onChange = { pct -> onPrefsChange { it.copy(positionPct = pct) } },
                label = { pct -> stringResource(R.string.setup_handle_position, pct) },
            )

            Text(stringResource(R.string.setup_handle_size), style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                options = listOf(
                    HandleSize.S to stringResource(R.string.setup_handle_size_s),
                    HandleSize.M to stringResource(R.string.setup_handle_size_m),
                    HandleSize.L to stringResource(R.string.setup_handle_size_l),
                ),
                selected = prefs.size,
                onSelect = { size -> onPrefsChange { it.copy(size = size) } },
            )

            PercentSlider(
                stored = prefs.opacityPct,
                range = Prefs.OPACITY_MIN_PCT..Prefs.OPACITY_MAX_PCT,
                onChange = { pct -> onPrefsChange { it.copy(opacityPct = pct) } },
                label = { pct ->
                    if (pct <= 0) {
                        stringResource(R.string.setup_handle_opacity_off)
                    } else {
                        stringResource(R.string.setup_handle_opacity, pct)
                    }
                },
            )
        }
    }
}

/**
 * Whole-percent slider that saves live while dragging. It compares against the last value it
 * sent, held in state, because the callbacks can see a stale [stored] between recompositions; and
 * it sends the final value on release, so what the user sees is what ends up stored.
 */
@Composable
private fun PercentSlider(
    stored: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    label: @Composable (Int) -> String,
) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    var lastSent by remember { mutableStateOf<Int?>(null) }
    val shown = dragValue ?: stored.toFloat()
    Text(label(shown.roundToInt()), style = MaterialTheme.typography.labelLarge)
    Slider(
        value = shown,
        onValueChange = { value ->
            dragValue = value
            val pct = value.roundToInt()
            if (pct != lastSent) {
                lastSent = pct
                onChange(pct)
            }
        },
        onValueChangeFinished = {
            dragValue?.roundToInt()?.let { final -> if (final != lastSent) onChange(final) }
            dragValue = null
            lastSent = null
        },
        valueRange = range.first.toFloat()..range.last.toFloat(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label)
            }
        }
    }
}

/** Explains the Quick Settings tile; on Android 13+ offers the system "Add tile" prompt. */
@Composable
internal fun TileCard(
    canRequestAddTile: Boolean,
    onAddTile: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_tile_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.setup_tile_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (canRequestAddTile) {
                OutlinedButton(
                    onClick = onAddTile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.setup_tile_button))
                }
            } else {
                Text(
                    text = stringResource(R.string.setup_tile_steps),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
