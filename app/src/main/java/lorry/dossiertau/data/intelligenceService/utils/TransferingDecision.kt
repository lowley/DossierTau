package lorry.dossiertau.data.intelligenceService.utils

import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture

sealed class TransferingDecision(
    val filePath: TauPath
) {

    data class CREATEFILE(
        val eventFilePath: TauPath,
        val modificationDate: TauDate,
    ): TransferingDecision(eventFilePath)
    data class DELETEFILE(val eventFilePath: TauPath): TransferingDecision(eventFilePath)




}