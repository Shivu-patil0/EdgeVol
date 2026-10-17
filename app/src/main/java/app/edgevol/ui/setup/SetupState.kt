package app.edgevol.ui.setup

import app.edgevol.system.SetupStatus

/** What the setup screen shows. Pure: no Android types, unit-testable. */
enum class SetupState {
    /** The accessibility service is switched off in Settings. */
    Off,

    /** Switched on in Settings, but the service process is not connected (e.g. cleared from Recents). */
    EnabledNotRunning,

    /** Switched on and connected: the handle really exists. */
    Running,
}

/** The Settings toggle wins: a connected service that is switched off is still [SetupState.Off]. */
fun SetupStatus.toSetupState(): SetupState = when {
    !serviceEnabled -> SetupState.Off
    serviceRunning -> SetupState.Running
    else -> SetupState.EnabledNotRunning
}
