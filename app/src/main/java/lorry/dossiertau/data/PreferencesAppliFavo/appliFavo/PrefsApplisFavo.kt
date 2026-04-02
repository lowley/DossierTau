package lorry.folder.items.dossiersigma.external.userPreferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import lorry.dossiertau.json
import lorry.dossiertau.usecases.applicationFavorites.support.Favorite

val Context.dataStore by preferencesDataStore(name = "prefs_applifavos")

open class PrefsAppliFavo(
    private val context: Context
) : IPrefsAppliFavo {

    private val dataStore = context.applicationContext.dataStore
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private val APPLI_FAVORITES_FOLDERS_KEY = stringSetPreferencesKey("appli_favorites_folders")
    }

    override val appliFavosFlow: StateFlow<List<Favorite>> = dataStore.data
        .map { preferences ->
            val rawSet = preferences[APPLI_FAVORITES_FOLDERS_KEY] ?: emptySet()
            val favoritesSet = rawSet.map { data ->
                val favorite = json.decodeFromString<Favorite>(data)
                favorite
            }

            favoritesSet
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    override suspend fun appliFavos(): List<Favorite> {
        return withContext(Dispatchers.IO) {
            var destinationFolders = emptyList<Favorite>()
            val item = appliFavosFlow.first()
            destinationFolders = item

            return@withContext destinationFolders
        }
    }

    override suspend fun saveAppliFavos(values: List<Favorite>) {
        context.dataStore.edit { preferences ->
            val stringValues = values.map {
                json.encodeToString(Favorite.serializer(), it)
            }

            preferences[APPLI_FAVORITES_FOLDERS_KEY] = stringValues.toSet()
        }
    }

    override suspend fun addAppliFavo(favorite: Favorite) = withContext(Dispatchers.IO) {
        var appliFavos = mutableSetOf<Favorite>()
        val item = appliFavosFlow.first()
        appliFavos = item.toMutableSet()

        appliFavos.add(favorite)
        saveAppliFavos(appliFavos.toList())
    }

    override suspend fun removeAppliFavo(favorite: Favorite) = withContext(Dispatchers.IO) {
        var appliFavos = mutableSetOf<Favorite>()
        val item = appliFavosFlow.first()
        appliFavos = item.toMutableSet()

        val newOnes = appliFavos.filter { it.fullPath != favorite.fullPath }
        saveAppliFavos(newOnes.toList())
    }
}