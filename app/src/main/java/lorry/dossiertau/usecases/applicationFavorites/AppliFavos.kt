package lorry.dossiertau.usecases.applicationFavorites

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.toFavorite
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.name
import lorry.dossiertau.usecases.applicationFavorites.support.AFRepo
import lorry.dossiertau.usecases.applicationFavorites.support.Favorite
import lorry.folder.items.dossiersigma.external.userPreferences.PrefsAppliFavo
import org.koin.java.KoinJavaComponent.inject
import kotlin.collections.any

class AppliFavos {
    val repo: AFRepo by inject(AFRepo::class.java)
    val vm: AFVm by inject(AFVm::class.java)
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val prefsAppliFavo: PrefsAppliFavo by inject(PrefsAppliFavo::class.java)

    val appliFavorites = prefsAppliFavo.appliFavosFlow

    fun addFavorite(favorite: Favorite) {
        scope.launch {
            prefsAppliFavo.addAppliFavo(favorite)
        }
    }

    fun removeFavorite(favorite: Favorite) {
        scope.launch {
            prefsAppliFavo.removeAppliFavo(favorite)
        }
    }

    fun toggleApplicationFavorite(item: TauItem) {
        scope.launch {
            val favorite = item.toFavorite()
            val isApplicationfavorite = appliFavorites.first().any { it.fullPath == item.fullPath }

            if (isApplicationfavorite)
                removeFavorite(favorite)
            else addFavorite(favorite)
        }
    }
}

fun List<Favorite>.contains(item: TauItem): Boolean {
    return this.any { it.isEqualTo(item) }
}

