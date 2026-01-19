package lorry.folder.items.dossiersigma.external.userPreferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import lorry.dossiertau.json
import lorry.dossiertau.usecases.applicationFavorites.support.Favorite
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "prefs_applifavos")

open class PrefsAppliFavo(
    private val context: Context
) : IPrefsAppliFavo {

    private val dataStore = context.applicationContext.dataStore

    companion object {
        private val APPLI_FAVORITES_FOLDERS_KEY = stringSetPreferencesKey("appli_favorites_folders")
    }

    override val appliFavosFlow: Flow<List<Favorite>> = dataStore.data
        .map { preferences ->
            val rawSet = preferences[APPLI_FAVORITES_FOLDERS_KEY] ?: emptySet()
            val favoritesSet = rawSet.map { data ->
                val favorite = json.decodeFromString<Favorite>(data)
                favorite
            }

            favoritesSet
        }

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