package lorry.dossiertau.usecases.folderContent.support

import android.graphics.Bitmap
import lorry.dossiertau.data.diskTransfer.TauRepoItem
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture

interface IFolderRepo {
    suspend fun getItemsInFullPath(tauPath: TauPath): List<TauRepoItem>
    suspend fun createSnapshotFor(folderPath: TauPath): Snapshot
    suspend fun loadThumbnail(itemPath: TauPath): TauPicture?

    suspend fun extractImageFromHtml(html: TauPath): Bitmap?
}