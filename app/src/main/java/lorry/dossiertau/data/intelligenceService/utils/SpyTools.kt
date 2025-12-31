package lorry.dossiertau.data.intelligenceService

import lorry.dossiertau.data.intelligenceService.utils.TauFileObserverInside
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import java.io.File

fun Spy.disableObservation() {
    if (fileObserver != null && fileObserver!!.started) {
        fileObserver!!.stopWatching()
    }
}
