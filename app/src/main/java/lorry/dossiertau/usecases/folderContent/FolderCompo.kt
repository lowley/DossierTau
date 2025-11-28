package lorry.dossiertau.usecases.folderContent

import arrow.core.None
import arrow.core.Option
import arrow.core.raise.fold
import arrow.core.toOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.computeParentFolderDate
import lorry.dossiertau.data.transfer.toTauItems
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.name
import lorry.dossiertau.support.littleClasses.parentPath
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo

class FolderCompo(
    val folderRepo: IFolderRepo
): IFolderCompo {

    private val _folderFlow = MutableStateFlow<Option<TauFolder>>(None)
    override val folderFlow = _folderFlow.asStateFlow()

    private fun changeFolderFlow(folder: Option<TauFolder>){
        println("DEBUG: changeFolderFlow($folder)")
        _folderFlow.update { folder }
    }

    /**
     * set folderFlow à "folder"
     */
    override fun setFolderFlow(folderFullPath: TauPath) {
        val repoItems = folderRepo.getItemsInFullPath(folderFullPath)
        val compoItems = repoItems.toTauItems()

        val folderDate = compoItems.computeParentFolderDate()

        val result = TauFolder(
            parentPath = folderFullPath.parentPath,
            name = folderFullPath.name,
            picture = TauPicture.NONE,
            modificationDate = folderDate,
            children = compoItems
        )
        println("DEBUG: result=$result")

        changeFolderFlow(result.toOption())
    }
}