package app.edgevol.system

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import app.edgevol.R
import app.edgevol.service.VolumeAccessibilityService
import app.edgevol.service.VolumeTileService

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
            showAutostartHelp = isXiaomiFamily(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty()),
            canRequestAddTile = canRequestAddTile(Build.VERSION.SDK_INT),
        )
    }

    /** Shows the system "Add tile" prompt (Android 13+); does nothing on older versions. */
    fun requestAddTile(context: Context) {
        if (Build.VERSION.SDK_INT < 33) return
        val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
        statusBar.requestAddTileService(
            ComponentName(context, VolumeTileService::class.java),
            context.getString(R.string.tile_label),
            Icon.createWithResource(context, R.drawable.ic_tile),
            context.mainExecutor,
        ) { }
    }

    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /** MIUI's Autostart list. Not public API: callers fall back to [appDetailsIntent] if it fails. */
    fun autostartSettingsIntent(): Intent = Intent().setComponent(
        ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        ),
    )
}
