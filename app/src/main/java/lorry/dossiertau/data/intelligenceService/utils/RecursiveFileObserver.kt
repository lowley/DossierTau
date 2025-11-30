package lorry.dossiertau.data.intelligenceService.utils

import android.os.FileObserver
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

open class RecursiveFileObserver(
    private val root: File,
    private val mask: Int =
        FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY or
                FileObserver.MOVED_FROM or FileObserver.MOVED_TO or
                FileObserver.ATTRIB or FileObserver.DELETE_SELF or FileObserver.MOVE_SELF,
    private val dispatch: (Event) -> Unit
) {
    data class Event(
        val event: Int,
        val isDir: Boolean,
        val dir: File,          // répertoire qui a émis l'évènement
        val childPath: String?, // chemin relatif (dans dir) ; null pour *_SELF
        val absolute: File      // fichier/dossier concerné (dir si *_SELF)
    )

    val started = AtomicBoolean(false)
    private val watchers = ConcurrentHashMap<String, FileObserver>() // key = chemin canonique

    fun startWatching() {
        if (!started.compareAndSet(false, true)) return
        require(root.isDirectory) { "Root must be a directory: ${root.absolutePath}" }
        // watcher pour root + tous les sous-dossiers existants
        addWatcher(root)
        bfs(root) { addWatcher(it) }
    }

    fun stopWatching() {
        if (!started.compareAndSet(true, false)) return
        watchers.values.forEach { runCatching { it.stopWatching() } }
        watchers.clear()
    }

    private fun addWatcher(dir: File) {
        if (!dir.isDirectory) return
        val key = dir.canonicalPath
        if (watchers.containsKey(key)) return

        val w = object : FileObserver(key, mask) {
            override fun onEvent(event: Int, path: String?) {
                val isDir = (event and FO_ISDIR) != 0
                val abs = if (path != null) File(dir, path) else dir

                // Dispatch vers l'appelant
                dispatch(Event(event, isDir, dir, path, abs))

                // Gestion dynamique des sous-dossiers
                val createdOrMovedIn = (event and (CREATE or MOVED_TO)) != 0
                val deletedChild     = (event and (DELETE or MOVED_FROM)) != 0
                val selfGone         = (event and (DELETE_SELF or MOVE_SELF)) != 0

                if (createdOrMovedIn && isDir && path != null) {
                    addWatcher(abs) // nouveau sous-dossier -> commencer à l'observer
                }
                if (deletedChild && isDir && path != null) {
                    removeWatcher(abs) // sous-dossier supprimé/déplacé -> arrêter
                }
                if (selfGone) {
                    removeWatcher(dir) // ce dossier lui-même a disparu/déménagé
                }
            }
        }
        w.startWatching()
        watchers[key] = w
    }

    private fun removeWatcher(dir: File) {
        val key = runCatching { dir.canonicalPath }.getOrElse { dir.absolutePath }
        watchers.remove(key)?.stopWatching()
    }

    /** Parcours en largeur de l’arborescence (répertoires uniquement). */
    private inline fun bfs(start: File, crossinline action: (File) -> Unit) {
        val q = ArrayDeque<File>()
        q.add(start)
        while (q.isNotEmpty()) {
            val d = q.removeFirst()
            val children = d.listFiles() ?: continue
            for (f in children) {
                if (f.isDirectory) {
                    action(f)
                    q.add(f)
                }
            }
        }
    }
}

/** Utilitaire lisible pour logger les flags FileObserver. */
fun fileObserverFlagsToString(event: Int): String = buildList {
    if ((event and FO_ISDIR) != 0) add("ISDIR")
    if ((event and FileObserver.CREATE) != 0) add("CREATE")
    if ((event and FileObserver.DELETE) != 0) add("DELETE")
    if ((event and FileObserver.MODIFY) != 0) add("MODIFY")
    if ((event and FileObserver.MOVED_FROM) != 0) add("MOVED_FROM")
    if ((event and FileObserver.MOVED_TO) != 0) add("MOVED_TO")
    if ((event and FileObserver.ATTRIB) != 0) add("ATTRIB")
    if ((event and FileObserver.OPEN) != 0) add("OPEN")
    if ((event and FileObserver.CLOSE_WRITE) != 0) add("CLOSE_WRITE")
    if ((event and FileObserver.CLOSE_NOWRITE) != 0) add("CLOSE_NOWRITE")
    if ((event and FileObserver.DELETE_SELF) != 0) add("DELETE_SELF")
    if ((event and FileObserver.MOVE_SELF) != 0) add("MOVE_SELF")
    if ((event and FO_Q_OVERFLOW) != 0) add("Q_OVERFLOW")
}.joinToString("|")

private const val FO_ISDIR      = 0x40000000   // inotify IN_ISDIR
private const val FO_Q_OVERFLOW = 0x00004000   // inotify IN_Q_OVERFLOW