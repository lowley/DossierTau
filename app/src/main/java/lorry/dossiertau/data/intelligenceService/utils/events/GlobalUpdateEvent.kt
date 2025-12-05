package lorry.dossiertau.data.intelligenceService.utils.events

import lorry.dossiertau.support.littleClasses.TauPath

data class GlobalUpdateEvent(
    override val path: TauPath,
): IUpdateEvent