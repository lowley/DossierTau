package lorry.dossiertau.data.intelligenceService.utils

import android.os.FileObserver
import arrow.core.toOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.utils.RecursiveFileObserver.Event
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserverInside.DISABLED.MASK
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserverInside.TauFileObserverData
import lorry.dossiertau.support.littleClasses.TauPath
import java.io.File


/**
 * INACTIVE peut devenir actif grâce à changeTarget()
 * DISABLED utilisé pour les tests seulement
 * var fileObserver: TauFileObserver = TauFileObserver.of(INACTIVE)
 */

data class TauFileObserver(var value: TauFileObserverInside) {

    fun assign(other: TauFileObserverInside) {
        this.value = other
    }

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        fun of(
            file: File,
            doOnEvent: suspend (event: Int, path: TauPath?) -> Unit
        ) = TauFileObserver(TauFileObserverInside.of(file, doOnEvent))

        fun of(inside: TauFileObserverInside) = TauFileObserver(inside)
    }

    fun changeTarget(
        path: TauPath,
        mask: Int = MASK,
        doOnEvent: (suspend (event: Int, path: TauPath?) -> Unit)
    ) {
        if (value is TauFileObserverInside.DISABLED)
            return

        if (value is TauFileObserverInside.INACTIVE) {
            val file = path.toFile().getOrNull()
            value = TauFileObserverInside.of(file ?: return, doOnEvent)
        }

        val doOnEventInternal = doOnEvent
            ?: (this as? TauFileObserverData)?.doOnEvent
            ?: return

        val lambda = { it: Event ->
            val event = it.event
            val path = TauPath.of(it.absolute.absolutePath)
            val scope = scope

            scope.launch {
                doOnEventInternal?.invoke(event, path)
            }

            Unit
        }

        value = TauFileObserverInside.of(
            file = path.toFile().getOrNull() ?: return,
            doOnEvent = doOnEvent ?: return,
        )

        value.changeTarget(
            path = path,
            mask = mask,
            lambda = lambda
        )
    }

    val started: Boolean
        get() = value.started

    fun startWatching() {
        if (!started)
            value.startWatching()
    }

    fun stopWatching() {
        value.stopWatching()
    }
}

sealed interface TauFileObserverInside {
    object INACTIVE : TauFileObserverInside

    object DISABLED : TauFileObserverInside

    data class TauFileObserverData(
        val file: File,
        val doOnEvent: suspend (event: Int, path: TauPath?) -> Unit
    ) : TauFileObserverInside

    companion object {
        fun of(
            file: File,
            doOnEvent: suspend (event: Int, path: TauPath?) -> Unit
        ) = TauFileObserverData(file, doOnEvent) as TauFileObserverInside

        //le vrai qui fait le boulot
        private var recursiveFileObserver: RecursiveFileObserver? = null
    }

    /**
     * ne démarre pas la surveillance.
     */
    fun changeTarget(
        path: TauPath,
        mask: Int = MASK,
        lambda: (event: Event) -> Unit
    ) {
        if (this is DISABLED)
            return

        val root = path.toFile() ?: return
        recursiveFileObserver?.stopWatching()

        recursiveFileObserver = RecursiveFileObserver(root.getOrNull()!!, MASK, lambda)
    }

    val MASK: Int
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

                recursiveFileObserver?.startWatching()
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





