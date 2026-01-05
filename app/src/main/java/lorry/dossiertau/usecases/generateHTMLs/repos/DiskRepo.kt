package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject
import java.io.File

class DiskRepo(): IDiskRepo {

    override suspend fun getLocalActresses(): List<Actress>{
        val root = File("/storage/emulated/0/Movies/sexe/filles")
        val actresses = root.listFiles()
            .filter { !it.isFile() }
            .map { it.name }
            .map { it.split("-") }
            .map { Actress(if (it.size >= 2) it[1] else it[0], it) }

        return actresses
    }

    override suspend fun getLocalSubjects(): List<Subject>{
        val root = File("/storage/emulated/0/Movies/sexe/fantasmes")
        val subjects = root.listFiles()
            .filter { !it.isFile() }
            .map { it.name }
            .map { it.split("-") }
            .map { Subject(if (it.size >= 2) it[1] else it[0], it) }

        return subjects
    }

    override suspend fun deleteAllHtmlsIn(root: TauPath) {

        val rootFile = root.toFile().getOrNull() ?: return
        val htmls = rootFile.listFiles {
            it.isFile && it.name.endsWith("html") && !it.name.startsWith(".")}

        htmls.onEach { html -> html.delete() }

        val subFolders = rootFile.listFiles() { it.isDirectory() }
        subFolders.onEach { subFolder -> deleteAllHtmlsIn(subFolder.path.toTauPath()) }
    }
}


