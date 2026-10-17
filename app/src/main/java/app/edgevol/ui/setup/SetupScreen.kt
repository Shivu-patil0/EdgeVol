package app.edgevol.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.edgevol.R
import app.edgevol.system.SetupStatus

@Composable
fun SetupScreen(
    status: SetupStatus,
    onEnableService: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onTestPanel: () -> Unit,
    onOpenAutostart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )

            val state = status.toSetupState()
            StatusCard(state = state)

            when (state) {
                SetupState.Running -> {
                    Text(
                        text = stringResource(R.string.setup_ready_message),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (status.showAutostartHelp) AutostartCard(onOpenAutostart = onOpenAutostart)
                }
                SetupState.EnabledNotRunning -> NotRunningContent(
                    showAutostartHelp = status.showAutostartHelp,
                    onOpenAutostart = onOpenAutostart,
                    onOpenSettings = onEnableService,
                )
                SetupState.Off -> {
                    ServiceOffContent(onEnableService = onEnableService)
                    if (status.showRestrictedSettingsHelp) {
                        RestrictedSettingsCard(onOpenAppInfo = onOpenAppInfo)
                    }
                }
            }

            TryItCard(onTestPanel = onTestPanel)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusCard(state: SetupState) {
    val colors = MaterialTheme.colorScheme
    val container = when (state) {
        SetupState.Running -> colors.primaryContainer
        SetupState.EnabledNotRunning -> colors.errorContainer
        SetupState.Off -> colors.surfaceVariant
    }
    val content = when (state) {
        SetupState.Running -> colors.onPrimaryContainer
        SetupState.EnabledNotRunning -> colors.onErrorContainer
        SetupState.Off -> colors.onSurfaceVariant
    }
    // One merged node, announced politely when the status changes.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = when (state) {
                    SetupState.Running -> CheckIcon
                    SetupState.EnabledNotRunning -> WarningIcon
                    SetupState.Off -> CrossIcon
                },
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = stringResource(
                    when (state) {
                        SetupState.Running -> R.string.setup_status_on
                        SetupState.EnabledNotRunning -> R.string.setup_status_not_running
                        SetupState.Off -> R.string.setup_status_off
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun NotRunningContent(
    showAutostartHelp: Boolean,
    onOpenAutostart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.setup_not_running_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (showAutostartHelp) AutostartCard(onOpenAutostart = onOpenAutostart)
        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_not_running_button),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            text = stringResource(R.string.setup_not_running_retry),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!showAutostartHelp) {
            Text(
                text = stringResource(R.string.setup_not_running_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AutostartCard(onOpenAutostart: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_autostart_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.setup_autostart_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = onOpenAutostart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.setup_autostart_button))
            }
        }
    }
}

@Composable
private fun ServiceOffContent(
    onEnableService: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.setup_disclosure_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.setup_disclosure_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onEnableService,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_enable_button),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun RestrictedSettingsCard(onOpenAppInfo: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_restricted_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.setup_restricted_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.setup_restricted_steps),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(
                onClick = onOpenAppInfo,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.setup_open_app_info_button))
            }
        }
    }
}

@Composable
private fun TryItCard(onTestPanel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_try_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.setup_try_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = onTestPanel,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.setup_try_button))
            }
        }
    }
}

// Hand-built vectors: material-icons is not a dependency. Tinted by Icon.
private val CheckIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Check",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.5f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(5f, 12.5f)
        lineTo(10f, 17.5f)
        lineTo(19f, 7f)
    }.build()
}

private val CrossIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Cross",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.5f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(6f, 6f)
        lineTo(18f, 18f)
        moveTo(18f, 6f)
        lineTo(6f, 18f)
    }.build()
}

// Triangle with an exclamation mark.
private val WarningIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Warning",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(12f, 3.5f)
        lineTo(21.5f, 20f)
        lineTo(2.5f, 20f)
        close()
        moveTo(12f, 9.5f)
        lineTo(12f, 14.5f)
        moveTo(12f, 17.2f)
        lineTo(12f, 17.3f)
    }.build()
}
