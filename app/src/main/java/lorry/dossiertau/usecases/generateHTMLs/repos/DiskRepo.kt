package lorry.dossiertau.usecases.generateHTMLs.repos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject
import java.io.File

class DiskRepo(): IDiskRepo {

    override suspend fun getLocalActresses(): List<Actress>{
        val root = File("/storage/emulated/0/Movies/sexe/filles")
        val actresses = root.listFiles()
            .filter { !it.isFile() }
            .map { it.name.lowercase() }
            .map { it.split(",") }
            .map { Actress(if (it.size >= 2) it[1] else it[0], it) }

        return actresses
    }

    override suspend fun getLocalSubjects(): List<Subject>{
        val root = File("/storage/emulated/0/Movies/sexe/fantasmes")
        val subjects = root.listFiles()
            .filter { !it.isFile() }
            .map { it.name.lowercase() }
            .map { it.split(",") }
            .map { Subject(it[0], it) }

        return subjects
    }

    suspend fun getLocalActressesAndFileNames(): List<Pair<Actress, TauItemName>> {

        val fillesPaths = withContext(Dispatchers.IO) {"/storage/emulated/0/Movies/sexe/filles".toTauPath().toFile()
            .getOrNull()
            ?.listFiles()?.filter { it.isDirectory }
        }

        val result = mutableMapOf<Actress, TauItemName>()

        fillesPaths?.onEach { file ->
            val actress = file.name
                .split(",")
                .let { items ->
                    if (items.size == 1)
                        Actress(
                            name = items.first(),
                            shortcuts = listOf(items.first())
                        )
                    else
                        Actress(
                            name = items[1],
                            shortcuts = items
                        )
                }

            result[actress] = TauItemName(file.name)
        }

        return result.toList()
    }

    suspend fun getLocalSubjectsAndFileNames(): List<Pair<Subject, Set<TauItemName>>> {

        val subjectsPaths = withContext(Dispatchers.IO) {
            "/storage/emulated/0/Movies/sexe/fantasmes".toTauPath().toFile()
                .getOrNull()
                ?.listFiles()?.filter { it.isDirectory }
        }

        val result = mutableMapOf<Subject, List<TauItemName>>()

        subjectsPaths?.onEach { file ->
            val subject = file.name
                .split(",")
                .let { items ->
                    if (items.size == 1)
                        Subject(
                            name = items.first(),
                            shortcuts = listOf(items.first())
                        )
                    else Subject(
                        name = items.first(),
                        shortcuts = items
                    )
                }

            val existingOne = result[subject]
            val newOne = existingOne?.plus(TauItemName(file.name)) ?: listOf(TauItemName(file.name))
            result[subject] = newOne
        }

        return result.map { it.key to it.value.toSet() }
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


