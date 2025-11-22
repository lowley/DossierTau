package lorry.dossiertau.usecases.folderContent.support

import lorry.dossiertau.data.transfer.TauRepoItem
import lorry.dossiertau.support.littleClasses.TauPath

interface IFolderRepo {
    fun getItemsInFullPath(tauPath: TauPath): List<TauRepoItem>


}