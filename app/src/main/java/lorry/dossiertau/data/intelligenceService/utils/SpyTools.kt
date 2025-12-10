package lorry.dossiertau.data.intelligenceService

import lorry.dossiertau.data.intelligenceService.utils.TauFileObserver
import lorry.dossiertau.data.intelligenceService.utils.events.createIncomingEvent
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import java.io.File

fun Spy.disableObservation() {
    if (fileObserver != null && fileObserver!!.started) {
        fileObserver!!.stopWatching()
    }
}

fun Spy.changeObserverWith(folderPath: TauPath): TauFileObserver? {
    disableObservation()
    val result = TauFileObserver.of(
        file = File(folderPath.path),
        doOnEvent = { event, path ->
            path?.let {
                val incomingEvent = createIncomingEvent(event, path)?.let {
                    doOnEvent(it)
                }
            }
        }

    )

    return result
}