package lorry.dossiertau.ui.support.capsule.utilities

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.FileTime

/**
 * Écrit (optionnel) puis restaure le mtime d’un fichier de façon robuste.
 * - Utilise File.setLastModified en priorité (ce que ton app relit via File.lastModified()).
 * - Retombe sur NIO si besoin.
 * - Vérifie le résultat.
 */
object FileMTime {

    /** À appeler juste après tes écritures pour garantir que le FS est à jour. */
    fun flushAndSync(fos: FileOutputStream) {
        fos.flush()
        try { fos.fd.sync() } catch (_: Throwable) { /* best effort */ }
    }

    /** Remet le mtime et renvoie le mtime effectivement lu ensuite par File. */
    fun restoreLastModified(file: File, oldMillis: Long): Long {
        // 1) Première passe: API File (celle que tu utilises pour lire)
        val ok1 = try { file.setLastModified(oldMillis) } catch (_: Throwable) { false }

        // 2) Si échec ou valeur non conforme, tentative NIO (équivalent bas niveau)
        val after1 = file.lastModified()
        val needNio = !ok1 || (after1 != oldMillis && Math.abs(after1 - oldMillis) > 1500)
        if (needNio) {
            try {
                Files.setLastModifiedTime(file.toPath(), FileTime.fromMillis(oldMillis))
            } catch (_: Throwable) { /* ignore */ }
        }

        // 3) Lecture de contrôle (ce que **File** renverra ensuite dans ton explorateur)
        return file.lastModified()
//            .also {
//            // NB: certains FS arrondissent à la seconde -> tolérance ~1.5s
//        }
    }
}
