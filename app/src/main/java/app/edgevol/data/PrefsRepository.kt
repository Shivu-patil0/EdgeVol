package app.edgevol.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Own copies of the bounds (overlay's HandleDefaults is internal to that package and Float-typed).
private const val POSITION_MIN_PCT = 10
private const val POSITION_MAX_PCT = 90

private val SIDE_KEY = stringPreferencesKey("side")
private val POSITION_KEY = intPreferencesKey("position_pct")

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

    suspend fun update(transform: (Prefs) -> Prefs) {
        store.edit { mutable ->
            mutable.write(transform(mutable.toPrefs()))
        }
    }
}

internal fun Preferences.toPrefs(): Prefs {
    val defaults = Prefs()
    // asMap() avoids get(), which throws ClassCastException for a key stored with another type.
    val raw = asMap()
    val side = (raw[SIDE_KEY] as? String)
        ?.let { name -> HandleSide.entries.firstOrNull { it.name == name } }
        ?: defaults.side
    val position = raw[POSITION_KEY] as? Int ?: defaults.positionPct
    return Prefs(side = side, positionPct = position.coerceIn(POSITION_MIN_PCT, POSITION_MAX_PCT))
}

internal fun MutablePreferences.write(prefs: Prefs) {
    this[SIDE_KEY] = prefs.side.name
    this[POSITION_KEY] = prefs.positionPct.coerceIn(POSITION_MIN_PCT, POSITION_MAX_PCT)
}
