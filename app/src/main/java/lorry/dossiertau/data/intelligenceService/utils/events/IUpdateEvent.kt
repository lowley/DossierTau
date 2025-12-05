package lorry.dossiertau.data.intelligenceService.utils.events

import lorry.dossiertau.support.littleClasses.TauPath

interface IUpdateEvent {
    val path: TauPath
}
