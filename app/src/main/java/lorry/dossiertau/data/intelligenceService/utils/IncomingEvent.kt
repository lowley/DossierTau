package lorry.dossiertau.data.intelligenceService.utils

import android.os.FileObserver
import androidx.compose.foundation.text.input.rememberTextFieldState
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauDate

data class IncomingEvent(
    val eventType: EventType,
    val path: TauPath,
    val itemType: ItemType,
    val modificationDate: TauDate
)

sealed class EventType(val message: String) {
    object CREATE : EventType("création de fichier/dossier")
    object DELETE : EventType("suppression")
    object MODIFY : EventType("modification du contenu")
    object MOVED_FROM : EventType("déplacement/renommage de ce fichier, depuis ce dossier")
    object MOVED_TO : EventType("déplacement/renommage vers ce dossier")
    object CLOSE_WRITE : EventType("fermeture après écriture")
    object UNKNOWN : EventType("???")
    object ATTRIB: EventType("changement d'attibut sans réécriture")
    object DELETE_SELF: EventType("suppression de ce dossier suivi")
    object MOVE_SELF: EventType("déplacement de ce dossier suivi")
}

enum class ItemType{
    FILE,
    FOLDER
}

//* cette méthode a des effets de bord
fun createIncomingEvent(code: Int, path: TauPath): IncomingEvent? {

    val eventType = eventCodeToEventType(code)
    val file = path.toFile().getOrNull()
    if ((eventType !in listOf(EventType.DELETE, EventType.DELETE_SELF, EventType.MOVED_FROM)) &&
        file?.exists() != true)
        return null

    return IncomingEvent(
        eventType = eventType,
        path = path,
        itemType = if (path.toFile().getOrNull()!!.isFile) ItemType.FILE else ItemType.FOLDER,
        modificationDate = file?.lastModified().toTauDate(),
    )
}

fun eventCodeToEventType(code: Int): EventType {
// On neutralise le bit ISDIR et on garde seulement les bits d'événements.
    val e = code and FileObserver.ALL_EVENTS

    return when {
        (e and FileObserver.DELETE_SELF) != 0 -> EventType.DELETE_SELF
        (e and FileObserver.MOVE_SELF)   != 0 -> EventType.MOVE_SELF

        (e and FileObserver.MOVED_FROM)  != 0 -> EventType.MOVED_FROM
        (e and FileObserver.MOVED_TO)    != 0 -> EventType.MOVED_TO

        (e and FileObserver.CREATE)      != 0 -> EventType.CREATE
        (e and FileObserver.DELETE)      != 0 -> EventType.DELETE

        (e and FileObserver.CLOSE_WRITE) != 0 -> EventType.CLOSE_WRITE
        (e and FileObserver.MODIFY)      != 0 -> EventType.MODIFY
        (e and FileObserver.ATTRIB)      != 0 -> EventType.ATTRIB

        else -> EventType.UNKNOWN
    }
}

