package lorry.dossiertau.data.intelligenceService.utils.events

import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture

data class GlobalSpyLevel(
    override val path: TauPath,
    val items: List<GlobalItem>
): ISpyLevel

data class GlobalItem(
    val path: TauPath,
    val itemType: ItemType,
    val modificationDate: TauDate,
    val itemId: FileId,
    val picture: TauPicture?,
    val memo: String?
)