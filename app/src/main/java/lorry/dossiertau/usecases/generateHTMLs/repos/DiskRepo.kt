package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject
import java.io.File

class DiskRepo(): IDiskRepo {

    override suspend fun getLocalActresses(): List<Actress>{
        val root = File("/storage/emulated/0/Movies/sexe/filles")
        val actresses = root.listFiles()
            .filter { !it.isFile() }
            .map { it.name }
            .map { it.split(".") }
            .map { Actress(it[0], it.drop(1)) }

        return actresses
    }

    override suspend fun getLocalSubjects(): List<Subject>{
        val root = File("/storage/emulated/0/Movies/sexe/fantasmes")
        val subjects = root.listFiles()
            .filter { !it.isFile() }
            .map { it.name }
            .map { it.split(".") }
            .map { Subject(it[0], it.drop(1)) }

        return subjects
    }


}