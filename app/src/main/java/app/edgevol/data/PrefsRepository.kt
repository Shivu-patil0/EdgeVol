package app.edgevol.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.edgevol.data.Prefs.Companion.OPACITY_MAX_PCT
import app.edgevol.data.Prefs.Companion.OPACITY_MIN_PCT
import app.edgevol.data.Prefs.Companion.POSITION_MAX_PCT
import app.edgevol.data.Prefs.Companion.POSITION_MIN_PCT
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val SIDE_KEY = stringPreferencesKey("side")
private val POSITION_KEY = intPreferencesKey("position_pct")
private val SIZE_KEY = stringPreferencesKey("size")
private val OPACITY_KEY = intPreferencesKey("opacity_pct")
private val ENABLED_KEY = booleanPreferencesKey("handle_enabled")

/** One DataStore instance per process for the "handle" file. */
private val Context.handleStore: DataStore<Preferences> by preferencesDataStore(
    name = "handle",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class PrefsRepository internal constructor(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.handleStore)

    /**
     * Missing, unknown or corrupt values, and an [IOException] while reading, fall back to the
     * defaults. Any other exception from the store is rethrown to the collector.
     */
    val prefs: Flow<Prefs> = store.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { it.toPrefs() }

    /** Applies [transform] to the stored prefs and returns what was stored. */
    suspend fun update(transform: (Prefs) -> Prefs): Prefs =
        store.edit { mutable ->
            mutable.write(transform(mutable.toPrefs()))
        }.toPrefs()
}

internal fun Preferences.toPrefs(): Prefs {
    val defaults = Prefs()
    // asMap() avoids get(), which throws ClassCastException for a key stored with another type.
    val raw = asMap()
    val side = (raw[SIDE_KEY] as? String)
        ?.let { name -> HandleSide.entries.firstOrNull { it.name == name } }
        ?: defaults.side
    val position = raw[POSITION_KEY] as? Int ?: defaults.positionPct
    val size = (raw[SIZE_KEY] as? String)
        ?.let { name -> HandleSize.entries.firstOrNull { it.name == name } }
        ?: defaults.size
    val opacity = raw[OPACITY_KEY] as? Int ?: defaults.opacityPct
    // Builds before the "Show handle" switch hid the handle with opacity 0; read that as switched off.
    val enabled = raw[ENABLED_KEY] as? Boolean ?: (opacity > 0)
    return Prefs(
        side = side,
        positionPct = position.coerceIn(POSITION_MIN_PCT, POSITION_MAX_PCT),
        size = size,
        opacityPct = if (opacity <= 0) defaults.opacityPct else opacity.coerceIn(OPACITY_MIN_PCT, OPACITY_MAX_PCT),
        handleEnabled = enabled,
    )
}

internal fun MutablePreferences.write(prefs: Prefs) {
    this[SIDE_KEY] = prefs.side.name
    this[POSITION_KEY] = prefs.positionPct.coerceIn(POSITION_MIN_PCT, POSITION_MAX_PCT)
    this[SIZE_KEY] = prefs.size.name
    this[OPACITY_KEY] = prefs.opacityPct.coerceIn(OPACITY_MIN_PCT, OPACITY_MAX_PCT)
    this[ENABLED_KEY] = prefs.handleEnabled
}
