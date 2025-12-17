package lorry.dossiertau.data.intelligenceService.utils

import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath

sealed class CIALevel(
    val itemPath: TauPath
) {

    data class CreateItem(
        val eventPath: TauPath,
        val modificationDate: TauDate,
        val itemType: ItemType,
    ): CIALevel(eventPath)

    data class DeleteItem(
        val eventPath: TauPath,
        val modificationDate: TauDate,
        val itemType: ItemType,
    ): CIALevel(eventPath)

    data class ModifyItem(
        val eventPath: TauPath,
        val modificationDate: TauDate,
        val itemType: ItemType,
    ): CIALevel(eventPath)

    data class GlobalRefresh(
        val eventPath: TauPath,
        val refreshDate: TauDate,
    ): CIALevel(eventPath)


}