package app.edgevol.ui.setup

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.edgevol.R
import app.edgevol.data.HandleSide
import app.edgevol.data.HandleSize
import app.edgevol.data.Prefs
import app.edgevol.system.SetupStatus
import app.edgevol.ui.theme.EdgeVolTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val calls = mutableListOf<String>()

    // The screen sends transforms; applying them here like MainActivity does lets the UI react.
    private var prefs by mutableStateOf(Prefs())

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun show(status: SetupStatus, initial: Prefs = Prefs()) {
        prefs = initial
        compose.setContent {
            EdgeVolTheme {
                SetupScreen(
                    status = status,
                    prefs = prefs,
                    onEnableService = { calls += "enable" },
                    onOpenAppInfo = { calls += "appInfo" },
                    onTestPanel = { calls += "panel" },
                    onOpenAutostart = { calls += "autostart" },
                    onAddTile = { calls += "addTile" },
                    onPrefsChange = { transform -> prefs = transform(prefs) },
                )
            }
        }
    }

    private fun click(id: Int) {
        compose.onNodeWithText(text(id)).performScrollTo().performClick()
    }

    private fun assertShown(id: Int, vararg args: Any) {
        compose.onNodeWithText(text(id, *args)).assertExists()
    }

    private fun assertNotShown(id: Int) {
        compose.onNodeWithText(text(id)).assertDoesNotExist()
    }

    @Test
    fun `off shows the disclosure and Enable opens accessibility settings`() {
        show(SetupStatus(serviceEnabled = false))

        assertShown(R.string.setup_status_off)
        assertShown(R.string.setup_disclosure_title)
        assertNotShown(R.string.setup_handle_title)
        assertNotShown(R.string.setup_restricted_title)
        click(R.string.setup_enable_button)

        assertEquals(listOf("enable"), calls)
    }

    @Test
    fun `off on Android 13+ also explains restricted settings`() {
        show(SetupStatus(serviceEnabled = false, showRestrictedSettingsHelp = true))

        assertShown(R.string.setup_restricted_title)
        click(R.string.setup_open_app_info_button)

        assertEquals(listOf("appInfo"), calls)
    }

    @Test
    fun `enabled but not running explains it and opens settings`() {
        show(SetupStatus(serviceEnabled = true, serviceRunning = false))

        assertShown(R.string.setup_status_not_running)
        assertShown(R.string.setup_not_running_hint)
        assertNotShown(R.string.setup_autostart_title)
        assertNotShown(R.string.setup_handle_title)
        click(R.string.setup_not_running_button)

        assertEquals(listOf("enable"), calls)
    }

    @Test
    fun `not running on Xiaomi shows the Keep EdgeVol running card instead of the hint`() {
        show(SetupStatus(serviceEnabled = true, serviceRunning = false, showAutostartHelp = true))

        assertShown(R.string.setup_autostart_title)
        assertNotShown(R.string.setup_not_running_hint)
        click(R.string.setup_autostart_button)

        assertEquals(listOf("autostart"), calls)
    }

    @Test
    fun `running shows where the handle is and the handle settings`() {
        show(SetupStatus(serviceEnabled = true, serviceRunning = true))

        assertShown(R.string.setup_status_on)
        assertShown(R.string.setup_ready_message)
        assertNotShown(R.string.setup_ready_hidden_message)
        assertShown(R.string.setup_handle_title)
        assertNotShown(R.string.setup_autostart_title)
        compose.onNodeWithText(text(R.string.setup_handle_show)).assertIsOn()
    }

    @Test
    fun `running on Xiaomi keeps the Keep EdgeVol running card`() {
        show(SetupStatus(serviceEnabled = true, serviceRunning = true, showAutostartHelp = true))

        assertShown(R.string.setup_autostart_title)
    }

    @Test
    fun `Show handle switch hides and shows the handle`() {
        show(SetupStatus(serviceEnabled = true, serviceRunning = true))

        click(R.string.setup_handle_show)
        assertEquals(false, prefs.handleEnabled)
        compose.onNodeWithText(text(R.string.setup_handle_show)).assertIsOff()
        assertShown(R.string.setup_ready_hidden_message)
        assertNotShown(R.string.setup_ready_message)

        click(R.string.setup_handle_show)
        assertEquals(true, prefs.handleEnabled)
    }

    @Test
    fun `side and size choices change the prefs`() {
        show(SetupStatus(serviceEnabled = true, serviceRunning = true))

        click(R.string.setup_handle_side_left)
        click(R.string.setup_handle_size_l)

        assertEquals(HandleSide.LEFT, prefs.side)
        assertEquals(HandleSize.L, prefs.size)
        compose.onNodeWithText(text(R.string.setup_handle_side_left)).assertIsSelected()
        compose.onNodeWithText(text(R.string.setup_handle_size_l)).assertIsSelected()
    }

    @Test
    fun `height and opacity sliders save whole percents and update their labels`() {
        show(SetupStatus(serviceEnabled = true, serviceRunning = true), Prefs(positionPct = 40, opacityPct = 35))
        val sliders = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        sliders[0].performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(62.4f) }
        sliders[1].performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(80f) }

        assertEquals(62, prefs.positionPct)
        assertEquals(80, prefs.opacityPct)
        assertShown(R.string.setup_handle_position, 62)
        assertShown(R.string.setup_handle_opacity, 80)
    }

    @Test
    fun `tile card offers the system prompt on Android 13+`() {
        show(SetupStatus(serviceEnabled = true, serviceRunning = true, canRequestAddTile = true))

        assertNotShown(R.string.setup_tile_steps)
        click(R.string.setup_tile_button)

        assertEquals(listOf("addTile"), calls)
    }

    @Test
    fun `tile card explains the manual steps before Android 13`() {
        show(SetupStatus(serviceEnabled = false, canRequestAddTile = false))

        assertShown(R.string.setup_tile_steps)
        assertNotShown(R.string.setup_tile_button)
    }

    @Test
    fun `Try it shows the panel in every state`() {
        show(SetupStatus(serviceEnabled = false))

        click(R.string.setup_try_button)

        assertEquals(listOf("panel"), calls)
        assertEquals(1, compose.onAllNodesWithText(text(R.string.setup_try_button)).fetchSemanticsNodes().size)
    }
}
