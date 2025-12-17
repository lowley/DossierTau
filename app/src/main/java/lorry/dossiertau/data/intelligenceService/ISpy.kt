package lorry.dossiertau.data.intelligenceService

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import lorry.dossiertau.data.intelligenceService.utils.events.ISpyLevel
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath

interface ISpy {

    ///////////////////////////////////////////////////////////////////////
    // évènements créés par l'espion suite à une opération sur le disque //
    ///////////////////////////////////////////////////////////////////////
    val updateEventFlow: SharedFlow<ISpyLevel>
    fun emitSpyLevel(event: ISpyLevel)


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
    fun emitFake_CREATEITEM(fileToEmit: TauPath, itemType: ItemType, modificationDate: TauDate)
    fun emitFake_DELETEITEM(itemToEmit: TauPath, itemType: ItemType, modificationDate: TauDate)
    fun emitFake_MODIFYITEM(itemToEmit: TauPath, itemType: ItemType, modificationDate: TauDate)
    fun emitFake_MOVEDFROM(itemToEmit: TauPath, itemType: ItemType, modificationDate: TauDate)
}