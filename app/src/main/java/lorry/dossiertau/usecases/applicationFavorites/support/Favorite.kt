package lorry.dossiertau.usecases.applicationFavorites.support

import kotlinx.serialization.Serializable
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.toFavorite
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.name

@Serializable
sealed class Favorite(
) {
    @Serializable
    object EMPTY: Favorite()

    @Serializable
    data class Data(
        val _fullPath: TauPath,
        val _picture: TauPicture,
    ): Favorite() {

    }

    inline val fullPath: TauPath get() = (this as? Data)?._fullPath ?: TauPath.EMPTY
    inline val picture: TauPicture get() = (this as? Data)?._picture ?: TauPicture.NONE
    inline val name: TauItemName get() = fullPath.name

    fun invoke(): Favorite {
        return EMPTY
    }

    fun invoke(
        fullPath: TauPath = TauPath.EMPTY,
        picture: TauPicture = TauPicture.NONE
    ): Favorite {
        return Data(fullPath, picture)
    }

    fun isEqualTo(item: TauItem): Boolean = fullPath == item.fullPath
}

