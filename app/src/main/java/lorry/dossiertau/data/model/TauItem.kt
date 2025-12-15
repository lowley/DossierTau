package lorry.dossiertau.data.model

import kotlinx.serialization.Serializable
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
sealed interface TauItem {}

// Contrat commun uniquement pour les variantes Data :
interface TauDataCommon {
    val id: TauIdentifier
    val parentPath: TauPath
    val name: TauItemName
    val picture: TauPicture
    val modificationDate: TauDate

    val fullPath: TauPath
        get() = parentPath.normalizeWithSlash().appendToTauPath(name)
}

fun Collection<TauItem>.files() = this.filterIsInstance<TauFile>()
fun Collection<TauItem>.folders() = this.filterIsInstance<TauFolder>()

fun TauItem.isFolder() = this is TauFolder
fun TauItem.isFile() = this is TauFile

fun Collection<TauItem>.computeParentFolderDate(): TauDate {
    if (this.isEmpty())
        return TauDate.now()

    val result = this.map { it.modificationDate }.maxBy { it.value }
    return result
}

fun List<TauItem>.sameContentAs(other: List<TauItem>): Boolean {

    if (this.size != other.size)
        return false

    val mine = this.sortedBy { item -> item.name.value }
    val theirs = other.sortedBy { item -> item.name.value }
    val pairs = mine.zip(theirs)

    return pairs.all { (item1, item2) ->
        item1.sameAs(item2)
    }
}

@OptIn(ExperimentalUuidApi::class)
fun TauItem.sameAs(other: TauItem): Boolean {

    var result = (this is TauFolder && other is TauFolder) || (this is TauFile && other is TauFile)
    if (result == false)
        return false

    if (this is TauFolder) {
        val folder1 = this as TauFolder
        val folder2 = other as TauFolder

        result = result && folder1.children.sameContentAs(folder2.children)
        if (result == false)
            return false

        val neutral1 = folder1.asData?.copy(
            id = TauIdentifier(Uuid.NIL),
            children = emptyList()
        )
        val neutral2 = folder1.asData?.copy(
            id = TauIdentifier(Uuid.NIL),
            children = emptyList())

        result = result && neutral1 == neutral2
        return result

    } else
    //TauFile n'override aucune propriété
        return true
}

inline val TauItem.asDataCommon: TauDataCommon?
    get() = when (this) {
        is TauFolder -> this.asData
        is TauFile   -> this.asData
    }

inline val TauItem.name: TauItemName
    get() = asDataCommon?.name ?: TauItemName.EMPTY

val TauItem.fullPath: TauPath
    get() = asDataCommon?.fullPath ?: TauPath.EMPTY

//TODO TauDate.EMPTY
inline val TauItem.modificationDate: TauDate
    get() = asDataCommon?.modificationDate ?: TauDate(0L)

inline val TauItem.parentPath: TauPath?
    get() = asDataCommon?.parentPath