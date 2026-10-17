package app.edgevol.system

data class SetupStatus(
    val serviceEnabled: Boolean,
    /** True on Android 13+ where a sideloaded app's accessibility toggle may be restricted. */
    val showRestrictedSettingsHelp: Boolean = false,
)

internal fun shouldShowRestrictedSettingsHelp(sdkInt: Int): Boolean = sdkInt >= 33
