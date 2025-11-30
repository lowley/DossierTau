package lorry.dossiertau.usecases.folderContent.support

import lorry.dossiertau.data.diskTransfer.TauRepoItem
import lorry.dossiertau.support.littleClasses.TauPath

interface IFolderRepo {
    suspend fun getItemsInFullPath(tauPath: TauPath): List<TauRepoItem>


}