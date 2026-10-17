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
    PreviewSetup(SetupStatus(serviceEnabled = true))
}

@Preview(name = "Light", showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun SetupScreenLightPreview() {
    PreviewSetup(SetupStatus(serviceEnabled = true))
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
