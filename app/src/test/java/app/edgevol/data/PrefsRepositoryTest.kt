package app.edgevol.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Uses an injected temp-file DataStore: never the production per-process singleton. */
class PrefsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val side = stringPreferencesKey("side")
    private val position = intPreferencesKey("position_pct")

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun file() = File(tmp.root, "handle.preferences_pb")

    private fun store(
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = corruptionHandler,
        scope = scope,
        produceFile = { file() },
    )

    private fun read(repo: PrefsRepository): Prefs = runBlocking { repo.prefs.first() }

    // Prefs defaults

    @Test
    fun `Prefs defaults are RIGHT and 40`() {
        assertEquals(HandleSide.RIGHT, Prefs().side)
        assertEquals(40, Prefs().positionPct)
    }

    // Repository over a real (temp file) store

    @Test
    fun `empty store yields defaults`() {
        assertEquals(Prefs(), read(PrefsRepository(store())))
    }

    @Test
    fun `update then read round trips`() = runBlocking {
        val repo = PrefsRepository(store())
        repo.update { Prefs(HandleSide.LEFT, 65) }
        assertEquals(Prefs(HandleSide.LEFT, 65), repo.prefs.first())
    }

    @Test
    fun `values persist across store instances on the same file`() = runBlocking {
        PrefsRepository(store()).update { Prefs(HandleSide.LEFT, 25) }
        scope.cancel() // release the file before opening a second store on it
        val secondScope = CoroutineScope(Dispatchers.IO + Job())
        try {
            val second = PreferenceDataStoreFactory.create(scope = secondScope, produceFile = { file() })
            assertEquals(Prefs(HandleSide.LEFT, 25), PrefsRepository(second).prefs.first())
        } finally {
            secondScope.cancel()
        }
    }

    @Test
    fun `update clamps 0 and 200 into 10 through 90`() = runBlocking {
        val repo = PrefsRepository(store())
        repo.update { it.copy(positionPct = 0) }
        assertEquals(10, repo.prefs.first().positionPct)
        repo.update { it.copy(positionPct = 200) }
        assertEquals(90, repo.prefs.first().positionPct)
        repo.update { it.copy(positionPct = -50) }
        assertEquals(10, repo.prefs.first().positionPct)
    }

    @Test
    fun `update keeps the other field`() = runBlocking {
        val repo = PrefsRepository(store())
        repo.update { Prefs(HandleSide.LEFT, 70) }
        repo.update { it.copy(positionPct = 20) }
        assertEquals(Prefs(HandleSide.LEFT, 20), repo.prefs.first())
        repo.update { it.copy(side = HandleSide.RIGHT) }
        assertEquals(Prefs(HandleSide.RIGHT, 20), repo.prefs.first())
    }

    @Test
    fun `update transform receives the current stored prefs`() = runBlocking {
        val repo = PrefsRepository(store())
        repo.update { Prefs(HandleSide.LEFT, 55) }
        var seen: Prefs? = null
        repo.update {
            seen = it
            it
        }
        assertEquals(Prefs(HandleSide.LEFT, 55), seen)
    }

    @Test
    fun `stored out of range and negative positions are coerced on read`() = runBlocking {
        val store = store()
        val repo = PrefsRepository(store)

        store.edit { it[position] = 500 }
        assertEquals(90, repo.prefs.first().positionPct)
        store.edit { it[position] = -7 }
        assertEquals(10, repo.prefs.first().positionPct)
        store.edit { it[position] = 0 }
        assertEquals(10, repo.prefs.first().positionPct)
    }

    @Test
    fun `unknown side string reads as RIGHT and keeps the position`() = runBlocking {
        val store = store()
        store.edit {
            it[side] = "CENTRE"
            it[position] = 30
        }
        assertEquals(Prefs(HandleSide.RIGHT, 30), PrefsRepository(store).prefs.first())
    }

    @Test
    fun `wrong typed values read as defaults without throwing`() = runBlocking {
        val store = store()
        store.edit {
            it[intPreferencesKey("side")] = 3
            it[stringPreferencesKey("position_pct")] = "high"
        }
        assertEquals(Prefs(), PrefsRepository(store).prefs.first())
    }

    @Test
    fun `update on top of wrong typed values overwrites them with valid ones`() = runBlocking {
        val store = store()
        store.edit {
            it[intPreferencesKey("side")] = 3
            it[stringPreferencesKey("position_pct")] = "high"
        }
        val repo = PrefsRepository(store)
        repo.update { it.copy(side = HandleSide.LEFT) }
        assertEquals(Prefs(HandleSide.LEFT, 40), repo.prefs.first())
    }

    @Test
    fun `corrupted file with the replace-file handler reads defaults and stays writable`() = runBlocking {
        file().writeBytes(ByteArray(64) { 0xFF.toByte() })
        val repo = PrefsRepository(store(ReplaceFileCorruptionHandler { emptyPreferences() }))

        assertEquals(Prefs(), repo.prefs.first())
        repo.update { Prefs(HandleSide.LEFT, 80) }
        assertEquals(Prefs(HandleSide.LEFT, 80), repo.prefs.first())
    }

    @Test
    fun `corrupted file without a handler still reads defaults via the IOException fallback`() {
        file().writeBytes(ByteArray(64) { 0xFF.toByte() })
        assertEquals(Prefs(), read(PrefsRepository(store())))
    }

    // Pure mapping helpers

    @Test
    fun `toPrefs of empty preferences is the default`() {
        assertEquals(Prefs(), emptyPreferences().toPrefs())
    }

    @Test
    fun `toPrefs reads valid values`() {
        assertEquals(
            Prefs(HandleSide.LEFT, 77),
            preferencesOf(side to "LEFT", position to 77).toPrefs(),
        )
    }

    @Test
    fun `toPrefs coerces out of range positions`() {
        assertEquals(10, preferencesOf(position to -1).toPrefs().positionPct)
        assertEquals(90, preferencesOf(position to 91).toPrefs().positionPct)
        assertEquals(10, preferencesOf(position to Int.MIN_VALUE).toPrefs().positionPct)
        assertEquals(90, preferencesOf(position to Int.MAX_VALUE).toPrefs().positionPct)
    }

    @Test
    fun `toPrefs maps an unknown enum string to RIGHT`() {
        assertEquals(HandleSide.RIGHT, preferencesOf(side to "left").toPrefs().side)
        assertEquals(HandleSide.RIGHT, preferencesOf(side to "").toPrefs().side)
    }

    @Test
    fun `toPrefs tolerates keys stored with the wrong type`() {
        val prefs = preferencesOf(intPreferencesKey("side") to 1, stringPreferencesKey("position_pct") to "x")
        assertEquals(Prefs(), prefs.toPrefs())
    }

    @Test
    fun `write stores the names and clamped percent under the documented keys`() {
        val m = mutablePreferencesOf()
        m.write(Prefs(HandleSide.LEFT, 200))
        assertEquals("LEFT", m[side])
        assertEquals(90, m[position])

        m.write(Prefs(HandleSide.RIGHT, -3))
        assertEquals("RIGHT", m[side])
        assertEquals(10, m[position])
    }

    // M3: size and opacity

    private val sizeKey = stringPreferencesKey("size")
    private val opacityKey = intPreferencesKey("opacity_pct")

    @Test
    fun `Prefs defaults are size M and opacity 35`() {
        assertEquals(HandleSize.M, Prefs().size)
        assertEquals(35, Prefs().opacityPct)
    }

    @Test
    fun `handle sizes are 56, 72 and 120 dp tall`() {
        assertEquals(56, HandleSize.S.heightDp)
        assertEquals(72, HandleSize.M.heightDp)
        assertEquals(120, HandleSize.L.heightDp)
    }

    @Test
    fun `the Show handle switch decides whether the handle is hidden`() {
        assertEquals(false, Prefs().handleHidden)
        assertEquals(true, Prefs(handleEnabled = false).handleHidden)
        assertEquals(true, Prefs().handleEnabled)
    }

    @Test
    fun `toPrefs reads size and opacity`() {
        val p = preferencesOf(sizeKey to "L", opacityKey to 60).toPrefs()
        assertEquals(HandleSize.L, p.size)
        assertEquals(60, p.opacityPct)
    }

    @Test
    fun `toPrefs clamps opacity to 10 to 80`() {
        assertEquals(80, preferencesOf(opacityKey to 120).toPrefs().opacityPct)
        assertEquals(10, preferencesOf(opacityKey to 3).toPrefs().opacityPct)
    }

    @Test
    fun `opacity 0 from older builds reads as hidden with the default opacity`() {
        val p = preferencesOf(opacityKey to 0).toPrefs()
        assertEquals(false, p.handleEnabled)
        assertEquals(35, p.opacityPct)
    }

    @Test
    fun `a stored Show handle value wins over the old opacity 0 rule`() {
        val p = preferencesOf(opacityKey to 0, enabledKey to true).toPrefs()
        assertEquals(true, p.handleEnabled)
        assertEquals(false, preferencesOf(opacityKey to 50, enabledKey to false).toPrefs().handleEnabled)
    }

    @Test
    fun `toPrefs falls back to defaults for an unknown size or a wrongly typed opacity`() {
        val p = preferencesOf(sizeKey to "XL", stringPreferencesKey("opacity_pct") to "high").toPrefs()
        assertEquals(HandleSize.M, p.size)
        assertEquals(35, p.opacityPct)
        assertEquals(true, p.handleEnabled)
    }

    @Test
    fun `size, opacity and the Show handle switch survive a write and read round trip`() {
        val repo = PrefsRepository(store())
        runBlocking { repo.update { it.copy(size = HandleSize.S, opacityPct = 15, handleEnabled = false) } }
        val p = read(repo)
        assertEquals(HandleSize.S, p.size)
        assertEquals(15, p.opacityPct)
        assertEquals(false, p.handleEnabled)
    }

    @Test
    fun `saving a snap keeps the size and opacity chosen on the setup screen`() {
        val repo = PrefsRepository(store())
        runBlocking {
            repo.update { it.copy(size = HandleSize.L, opacityPct = 70) }
            repo.update { it.copy(side = HandleSide.LEFT, positionPct = 25) }
        }
        assertEquals(Prefs(HandleSide.LEFT, 25, HandleSize.L, 70), read(repo))
    }

    @Test
    fun `update returns what was stored, including earlier saves and clamping`() {
        val repo = PrefsRepository(store())
        val stored = runBlocking {
            repo.update { it.copy(side = HandleSide.LEFT) }
            repo.update { it.copy(opacityPct = 500) }
        }
        assertEquals(Prefs(side = HandleSide.LEFT, opacityPct = 80), stored)
    }

    @Test
    fun `back-to-back updates are applied in order, so the last value wins`() {
        val repo = PrefsRepository(store())
        val stored = runBlocking {
            repo.update { it.copy(handleEnabled = false) }
            repo.update { it.copy(handleEnabled = true) }
            repo.update { it.copy(handleEnabled = false) }
        }
        assertEquals(false, stored.handleEnabled)
        assertEquals(true, read(repo).handleHidden)
    }

    private val enabledKey = booleanPreferencesKey("handle_enabled")
}
