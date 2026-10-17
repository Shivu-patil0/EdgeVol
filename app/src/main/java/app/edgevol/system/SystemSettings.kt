package app.edgevol.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import app.edgevol.service.VolumeAccessibilityService

object SystemSettings {
    fun status(context: Context): SetupStatus {
        val enabled = runCatching {
            val value = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
            isServiceEnabled(value, context.packageName, VolumeAccessibilityService::class.java.name)
        }.getOrDefault(false)
        return SetupStatus(
            serviceEnabled = enabled,
            showRestrictedSettingsHelp = shouldShowRestrictedSettingsHelp(Build.VERSION.SDK_INT),
            serviceRunning = VolumeAccessibilityService.isRunning,
        )
    }

    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
