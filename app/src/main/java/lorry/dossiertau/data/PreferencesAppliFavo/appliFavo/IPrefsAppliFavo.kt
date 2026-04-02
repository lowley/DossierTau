package lorry.folder.items.dossiersigma.external.userPreferences

import kotlinx.coroutines.flow.StateFlow
import lorry.dossiertau.usecases.applicationFavorites.support.Favorite

interface IPrefsAppliFavo {

    val appliFavosFlow: StateFlow<List<Favorite>>

    suspend fun appliFavos(): List<Favorite>


    suspend fun saveAppliFavos(rawValues: List<Favorite>)
    suspend fun addAppliFavo(favorite: Favorite)
    suspend fun removeAppliFavo(favorite: Favorite)
}