package lorry.dossiertau.data.intelligenceService.utils.events

import android.os.FileObserver
import lorry.dossiertau.data.intelligenceService.utils.CIALevel
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.parentPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauDate

data class AtomicSpyLevel(
    val eventType: AtomicEventType,
    override val path: TauPath,
    val itemType: ItemType,
    val modificationDate: TauDate,
    val itemId: FileId,
    val picture: TauPicture?,
    val memo: String?
): ISpyLevel{

    override fun toString(): String {
        return "AtomicSpyLevel($eventType ↈ ${path.path} ↈ ${modificationDate.toddMMyyyyHHmmss()} ↈ $itemId)"
    }
}

internal val fileInsideReaction = { insidePath: TauPath, aroundPath: TauPath, potentialTransferringDecision: CIALevel ->
    if (insidePath.parentPath == aroundPath) potentialTransferringDecision else null
}

internal val selfReaction = { insidePath: TauPath, aroundPath: TauPath, potentialTransferringDecision: CIALevel ->
    if (insidePath == aroundPath) potentialTransferringDecision else null
}

sealed class AtomicEventType(
    val message: String,
    val reactWhenReceived: (insidePath:TauPath, aroundPath: TauPath, potentialTransferringDecision: CIALevel) -> CIALevel?
) {
    object CREATE : AtomicEventType("création de fichier/dossier", fileInsideReaction)
    object DELETE : AtomicEventType("suppression", fileInsideReaction)
    object MODIFY : AtomicEventType("modification du contenu", fileInsideReaction)

    override fun toString(): String {
        return when (this){
            is CREATE -> "➕"
            is MODIFY -> "⮂"
            is DELETE -> "➖"
        }
    }


}

enum class ItemType{
    FILE,
    FOLDER
}
