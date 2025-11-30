package lorry.dossiertau.data.intelligenceService

import lorry.dossiertau.data.intelligenceService.Spy
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserver
import lorry.dossiertau.data.intelligenceService.utils.createIncomingEvent
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import java.io.File

fun Spy.disableObservation() {
    if (fileObserver != null && fileObserver!!.started.get()) {
        fileObserver!!.stopWatching()
        fileObserver = null
    }
}

fun Spy.createObserverWith(folderPath: TauPath): TauFileObserver? = TauFileObserver(
    file = File(folderPath.path),
    doOnEvent = { event, path ->
        path?.let {
            val incomingEvent = createIncomingEvent(event, path)?.let {
                doOnEvent(it)
            }
        }
    }
)