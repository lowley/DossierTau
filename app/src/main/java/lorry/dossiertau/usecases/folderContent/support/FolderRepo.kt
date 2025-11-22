package lorry.dossiertau.usecases.folderContent.support

import arrow.core.Option
import kotlinx.coroutines.flow.StateFlow
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.transfer.TauRepoItem
import lorry.dossiertau.support.littleClasses.TauPath

class FolderRepo: IFolderRepo {

    override fun getItemsInFullPath(tauPath: TauPath): List<TauRepoItem> {

        return emptyList()
    }


}

