package lorry.dossiertau.data.intelligenceService.utils

import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath

sealed class TransferingDecision(
    val filePath: TauPath
) {

    data class CreateFile(
        val eventFilePath: TauPath,
        val modificationDate: TauDate,
    ): TransferingDecision(eventFilePath)

    data class DeleteFile(val eventFilePath: TauPath): TransferingDecision(eventFilePath)

    data class GlobalRefresh(
        val eventFilePath: TauPath,
    ): TransferingDecision(eventFilePath)


}