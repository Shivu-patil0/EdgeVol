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
)

internal fun shouldShowRestrictedSettingsHelp(sdkInt: Int): Boolean = sdkInt >= 33
