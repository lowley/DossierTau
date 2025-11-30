package lorry.dossiertau.data.intelligenceService.utils

import android.os.FileObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lorry.dossiertau.support.littleClasses.TauPath
import java.io.File

class TauFileObserver(
    val file: File,
    val doOnEvent: suspend (event: Int, path: TauPath?) -> Unit
) : RecursiveFileObserver(
    file,
        FileObserver.MOVED_FROM or
        FileObserver.MOVED_TO or
        FileObserver.CREATE or
        FileObserver.DELETE or
        FileObserver.MODIFY or
        FileObserver.ATTRIB or
        FileObserver.DELETE_SELF or
        FileObserver.MOVE_SELF or
        FileObserver.CLOSE_WRITE,
    {
        val event = it.event
        val path = TauPath.of(it.absolute.absolutePath)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            doOnEvent(event, path)
        }
    })