package app.edgevol

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.edgevol.audio.SystemVolume
import app.edgevol.system.SetupStatus
import app.edgevol.system.SystemSettings
import app.edgevol.ui.setup.SetupScreen
import app.edgevol.ui.theme.EdgeVolTheme

class MainActivity : ComponentActivity() {

    // Placeholder only: replaced in onCreate before setContent, so it is never drawn.
    private var status by mutableStateOf(SetupStatus(serviceEnabled = false))

    private val handler = Handler(Looper.getMainLooper())
    private val recheck = Runnable { status = SystemSettings.status(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        status = SystemSettings.status(this)
        val volume = SystemVolume(applicationContext)
        setContent {
            EdgeVolTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SetupScreen(
                        status = status,
                        onEnableService = { launchSafely(SystemSettings.accessibilitySettingsIntent()) },
                        onOpenAppInfo = { launchSafely(SystemSettings.appDetailsIntent(this@MainActivity)) },
                        onTestPanel = { volume.showPanel() },
                        onOpenAutostart = ::openAutostartSettings,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        status = SystemSettings.status(this)
        // A healthy service in a freshly started process connects slightly after the activity.
        handler.removeCallbacks(recheck)
        handler.postDelayed(recheck, RECHECK_DELAY_MS)
    }

    override fun onPause() {
        handler.removeCallbacks(recheck)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(recheck)
        super.onDestroy()
    }

    private fun launchSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.setup_error_settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    // MIUI's Autostart screen is not public API; fall back to App info, which also links to it.
    private fun openAutostartSettings() {
        try {
            startActivity(SystemSettings.autostartSettingsIntent())
        } catch (e: ActivityNotFoundException) {
            launchSafely(SystemSettings.appDetailsIntent(this))
        } catch (e: SecurityException) {
            launchSafely(SystemSettings.appDetailsIntent(this))
        }
    }

    private companion object {
        const val RECHECK_DELAY_MS = 1500L
    }
}
