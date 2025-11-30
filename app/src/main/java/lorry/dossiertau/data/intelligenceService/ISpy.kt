package lorry.dossiertau.data.intelligenceService

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import lorry.dossiertau.data.intelligenceService.utils.IncomingEvent
import lorry.dossiertau.data.intelligenceService.utils.ItemType
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath

interface ISpy {

    ///////////////////////////////////////////////////////////////////////
    // évènements créés par l'espion suite à une opération sur le disque //
    ///////////////////////////////////////////////////////////////////////
    val incomingEventFlow: SharedFlow<IncomingEvent>
    fun emitIncomingEvent(event: IncomingEvent)


    ////////////////////////////////////
    // interrupteur de fonctionnement //
    ////////////////////////////////////
    val enabledFlow : StateFlow<Boolean>

    fun startSurveillance()

    fun stopSurveillance()

    fun setSurveillance(enabled: Boolean)


    ////////////////////////
    // répertoire observé //
    ////////////////////////
    val observedFolderFlow: StateFlow<TauPath>
    fun setObservedFolder(folderPath: TauPath)


    ///////////
    // fakes //
    ///////////
    fun emitFake_CREATEFILE(fileToEmit: TauPath, itemType: ItemType, modificationDate: TauDate)
}