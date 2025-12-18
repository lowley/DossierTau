package lorry.dossiertau.usecases.folderContent.support

import lorry.dossiertau.data.diskTransfer.TauRepoItem
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.support.littleClasses.TauPath

interface IFolderRepo {
    suspend fun getItemsInFullPath(tauPath: TauPath): List<TauRepoItem>
    fun createSnapshotFor(folderPath: TauPath): Snapshot


}