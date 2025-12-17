package lorry.dossiertau.data.intelligenceService.utils.events

import android.os.FileObserver
import lorry.dossiertau.data.intelligenceService.utils.CIALevel
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.parentPath
import lorry.dossiertau.support.littleClasses.toTauDate

data class AtomicSpyLevel(
    val eventType: AtomicEventType,
    override val path: TauPath,
    val itemType: ItemType,
    val modificationDate: TauDate,
): ISpyLevel

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
    object MOVED_FROM : AtomicEventType("déplacement/renommage de ce fichier, depuis ce dossier", fileInsideReaction)
    object MOVED_TO : AtomicEventType("déplacement/renommage vers ce dossier", fileInsideReaction)
    object CLOSE_WRITE : AtomicEventType("fermeture après écriture", fileInsideReaction)
    object UNKNOWN : AtomicEventType("???", fileInsideReaction)
    object ATTRIB : AtomicEventType("changement d'attibut sans réécriture", fileInsideReaction)
    object DELETE_SELF: AtomicEventType("suppression de ce dossier suivi", selfReaction)
    object MOVE_SELF: AtomicEventType("déplacement de ce dossier suivi", selfReaction)
}

enum class ItemType{
    FILE,
    FOLDER
}

//* cette méthode a des effets de bord
fun createIncomingEvent(code: Int, path: TauPath): AtomicSpyLevel? {

    val eventType = code.toEventType()
    val file = path.toFile().getOrNull()
    if ((eventType !in listOf(AtomicEventType.DELETE, AtomicEventType.DELETE_SELF, AtomicEventType.MOVED_FROM)) &&
        file?.exists() != true)
        return null

    return AtomicSpyLevel(
        eventType = eventType,
        path = path,
        itemType = if (path.toFile().getOrNull()!!.isFile) ItemType.FILE else ItemType.FOLDER,
        modificationDate = file?.lastModified().toTauDate(),
    )
}

fun Int.toEventType(): AtomicEventType {
// On neutralise le bit ISDIR et on garde seulement les bits d'événements.
    val e = this and FileObserver.ALL_EVENTS

    return when {
        (e and FileObserver.DELETE_SELF) != 0 -> AtomicEventType.DELETE_SELF
        (e and FileObserver.MOVE_SELF)   != 0 -> AtomicEventType.MOVE_SELF

        (e and FileObserver.MOVED_FROM)  != 0 -> AtomicEventType.MOVED_FROM
        (e and FileObserver.MOVED_TO)    != 0 -> AtomicEventType.MOVED_TO

        (e and FileObserver.CREATE)      != 0 -> AtomicEventType.CREATE
        (e and FileObserver.DELETE)      != 0 -> AtomicEventType.DELETE

        (e and FileObserver.CLOSE_WRITE) != 0 -> AtomicEventType.CLOSE_WRITE
        (e and FileObserver.MODIFY)      != 0 -> AtomicEventType.MODIFY
        (e and FileObserver.ATTRIB)      != 0 -> AtomicEventType.ATTRIB

        else -> AtomicEventType.UNKNOWN
    }
}

