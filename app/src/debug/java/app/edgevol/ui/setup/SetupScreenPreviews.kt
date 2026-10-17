package app.edgevol.ui.setup

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.edgevol.system.SetupStatus
import app.edgevol.ui.theme.EdgeVolTheme

@Composable
private fun PreviewSetup(status: SetupStatus) {
    EdgeVolTheme {
        SetupScreen(
            status = status,
            onEnableService = {},
            onOpenAppInfo = {},
            onTestPanel = {},
            onOpenAutostart = {},
        )
    }
}

@Preview(name = "Service off (light)", showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun SetupScreenOffPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = false))
}

@Preview(name = "Service off + restricted help", showBackground = true, heightDp = 1100)
@Composable
private fun SetupScreenOffRestrictedPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = false, showRestrictedSettingsHelp = true))
}

@Preview(name = "Service on", showBackground = true)
@Composable
private fun SetupScreenOnPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = true, serviceRunning = true))
}

@Preview(name = "Enabled, not running (light)", showBackground = true, uiMode = UI_MODE_NIGHT_NO, heightDp = 900)
@Composable
private fun SetupScreenNotRunningLightPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = true, serviceRunning = false))
}

@Preview(name = "Enabled, not running (dark)", showBackground = true, uiMode = UI_MODE_NIGHT_YES, heightDp = 900)
@Composable
private fun SetupScreenNotRunningDarkPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = true, serviceRunning = false))
}

@Preview(name = "Light", showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun SetupScreenLightPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = true, serviceRunning = true))
}

@Preview(name = "Running (dark)", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SetupScreenRunningDarkPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = true, serviceRunning = true))
}

@Preview(name = "Dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SetupScreenDarkPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = false, showRestrictedSettingsHelp = true))
}

@Preview(name = "Font scale 2x", showBackground = true, fontScale = 2f, heightDp = 1400)
@Composable
private fun SetupScreenLargeFontPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = false, showRestrictedSettingsHelp = true))
}

@Preview(name = "Running on Xiaomi (Autostart card)", showBackground = true, heightDp = 1000)
@Composable
private fun SetupScreenRunningXiaomiPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = true, serviceRunning = true, showAutostartHelp = true))
}

@Preview(name = "Not running on Xiaomi (Autostart card)", showBackground = true, heightDp = 1100)
@Composable
private fun SetupScreenNotRunningXiaomiPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = true, serviceRunning = false, showAutostartHelp = true))
}
