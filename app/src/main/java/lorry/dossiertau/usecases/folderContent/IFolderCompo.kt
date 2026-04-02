package lorry.dossiertau.usecases.folderContent

import arrow.core.Option
import kotlinx.coroutines.flow.StateFlow
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.support.littleClasses.TauPath

interface IFolderCompo {

    val folderFlow: StateFlow<Option<TauFolder>>
    fun setFolderFlow(folder: TauPath)
    fun setFolderOrdering(ordering: Boolean)
    val folderPathFlow: StateFlow<Option<TauPath>>

    fun changeFolderFlow(folder: Option<TauFolder>)
    val ordering: StateFlow<Boolean>
    fun setOrdering(value: Boolean)
}