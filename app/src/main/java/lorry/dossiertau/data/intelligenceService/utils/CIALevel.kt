package lorry.dossiertau.data.intelligenceService.utils

import lorry.dossiertau.data.intelligenceService.utils.events.GlobalItem
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture

sealed class CIALevel(
    val itemPath: TauPath
) {

    data class CreateItem(
        val eventPath: TauPath,
        val modificationDate: TauDate,
        val itemType: ItemType,
        val itemId: FileId,
        val picture: TauPicture? = null,
        val memo: String? = null
    ): CIALevel(eventPath)

    data class DeleteItem(
        val eventPath: TauPath,
        val modificationDate: TauDate,
        val itemType: ItemType,
        val itemId: FileId,
        val picture: TauPicture? = null,
        val memo: String? = null
    ): CIALevel(eventPath)

    data class ModifyItem(
        val eventPath: TauPath,
        val modificationDate: TauDate,
        val itemType: ItemType,
        val itemId: FileId,
        val picture: TauPicture? = null,
        val memo: String? = null
    ): CIALevel(eventPath)

    data class GlobalRefresh(
        val eventPath: TauPath,
        val refreshDate: TauDate,
        val picture: TauPicture? = null,
        val memo: String? = null,
        val items: List<GlobalItem> = emptyList()
    ): CIALevel(eventPath)


}