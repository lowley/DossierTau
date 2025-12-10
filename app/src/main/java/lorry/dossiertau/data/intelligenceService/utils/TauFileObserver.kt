package lorry.dossiertau.data.intelligenceService.utils

import android.os.FileObserver
import androidx.compose.runtime.internal.rememberComposableLambda
import androidx.compose.ui.graphics.findFirstCubicRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.utils.RecursiveFileObserver.Event
import lorry.dossiertau.support.littleClasses.TauPath
import java.io.File


/**
 * INACTIVE peut devenir actif grâce à changeTarget()
 * DISABLED utilisé pour les tests seulement
 */
sealed interface TauFileObserver {
    object INACTIVE : TauFileObserver

    object DISABLED: TauFileObserver

    data class TauFileObserverData(
        val file: File,
        val doOnEvent: suspend (event: Int, path: TauPath?) -> Unit
    ) : TauFileObserver

    companion object {
        fun of(
            file: File,
            doOnEvent: suspend (event: Int, path: TauPath?) -> Unit
        ) = TauFileObserverData(file, doOnEvent) as TauFileObserver

        //le vrai qui fait le boulot
        private var recursiveFileObserver: RecursiveFileObserver? = null
    }

    /**
     * ne démarre pas la surveillance.
     */
    fun changeTarget(
        path: TauPath,
        mask: Int = MASK,
        doOnEvent: (suspend (event: Int, path: TauPath?) -> Unit)? = null
    ) {
        if (this is DISABLED)
            return

        val root = path.toFile() ?: return
        recursiveFileObserver?.stopWatching()

        val doOnEventInternal = doOnEvent
            ?: (this as? TauFileObserverData)?.doOnEvent
            ?: return

        val lambda = { it: Event ->
            val event = it.event
            val path = TauPath.of(it.absolute.absolutePath)
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            scope.launch {
                doOnEventInternal?.invoke(event, path)
            }

            Unit
        }

        recursiveFileObserver = RecursiveFileObserver(root.getOrNull()!!, MASK, lambda)
    }

    private val MASK: Int
        get() = FileObserver.MOVED_FROM or
                FileObserver.MOVED_TO or
                FileObserver.CREATE or
                FileObserver.DELETE or
                FileObserver.MODIFY or
                FileObserver.ATTRIB or
                FileObserver.DELETE_SELF or
                FileObserver.MOVE_SELF or
                FileObserver.CLOSE_WRITE

    val started: Boolean
        get() = when (this) {
            is INACTIVE -> false
            is DISABLED -> false
            is TauFileObserverData -> recursiveFileObserver?.started?.get() ?: false
        }

    fun startWatching() {
        when (this) {
            is INACTIVE -> Unit
            is DISABLED -> Unit
            is TauFileObserverData -> {
//                recursiveFileObserver = RecursiveFileObserver(root = this.file, mask = this.MASK) {
//                    val event = it.event
//                    val path = TauPath.of(it.absolute.absolutePath)
//                    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//
//                    scope.launch {
//                        doOnEvent(event, path)
//                    }
//                }

                this.startWatching()
            }
        }
    }

    fun stopWatching() {
        when (this) {
            is INACTIVE -> Unit
            is DISABLED -> Unit
            is TauFileObserverData -> recursiveFileObserver?.stopWatching()
        }
    }
}





