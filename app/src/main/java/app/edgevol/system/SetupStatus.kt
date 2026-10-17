package app.edgevol.system

/**
 * @property serviceRunning the accessibility service is connected in this process; false after the
 * app process was killed (e.g. cleared from Recents) until the user switches the service off and on.
 */
data class SetupStatus(
    val serviceEnabled: Boolean,
    /** True on Android 13+ where a sideloaded app's accessibility toggle may be restricted. */
    val showRestrictedSettingsHelp: Boolean = false,
    val serviceRunning: Boolean = false,
    /**
     * True on Xiaomi/Redmi/POCO (MIUI) phones, where the service only comes back by itself after
     * the phone kills the app (e.g. "clear all" in Recents) if Autostart is allowed for EdgeVol.
     */
    val showAutostartHelp: Boolean = false,
)

internal fun shouldShowRestrictedSettingsHelp(sdkInt: Int): Boolean = sdkInt >= 33

internal fun isXiaomiFamily(manufacturer: String, brand: String): Boolean =
    listOf(manufacturer, brand).any { value ->
        XIAOMI_FAMILY.any { it.equals(value.trim(), ignoreCase = true) }
    }

private val XIAOMI_FAMILY = listOf("Xiaomi", "Redmi", "POCO")
