package lorry.dossiertau.usecases.naming

import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Stuff
import lorry.dossiertau.usecases.generateHTMLs.support.Subject

/**
 * Etape 2 du pipeline CatCatch -> NAS.
 *
 * Responsabilite unique : calculer le nom final d'une video a partir des
 * personnes/sujets trouves sur la page et du referentiel local du telephone.
 * Aucune dependance reseau, NAS ou Android ici : la logique reste testable localement.
 */
object Naming {

    data class Request(
        val originalFileName: String,
        val pageActresses: List<String> = emptyList(),
        val pageSubjects: List<String> = emptyList(),
        val localActresses: List<Actress> = emptyList(),
        val localSubjects: List<Subject> = emptyList(),
    )

    data class Result(
        val fileName: String,
        val matchedActresses: List<Actress>,
        val matchedSubjects: List<Subject>,
    )

    fun compute(request: Request): Result {
        val actresses = match(request.pageActresses, request.localActresses)
        val subjects = match(request.pageSubjects, request.localSubjects)

        val finalName = (actresses + subjects)
            .fold(request.originalFileName) { currentName, stuff ->
                appendCanonicalShortcut(currentName, stuff)
            }
            .replace(" - HotMovies", "")

        return Result(
            fileName = finalName,
            matchedActresses = actresses,
            matchedSubjects = subjects,
        )
    }

    private fun <T : Stuff> match(pageValues: List<String>, localValues: List<T>): List<T> {
        val normalizedPageValues = pageValues
            .map(::normalize)
            .filter { it.isNotEmpty() }
            .toSet()

        return localValues
            .filter { local ->
                local.shortcuts.any { normalize(it) in normalizedPageValues } ||
                    normalize(local.name) in normalizedPageValues
            }
            .distinctBy { normalize(it.name) }
    }

    private fun appendCanonicalShortcut(fileName: String, stuff: Stuff): String {
        val canonicalShortcut = stuff.shortcuts
            .firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return fileName

        val parts = fileName.split(".")
        if (parts.size < 2) return "$fileName.$canonicalShortcut"

        val extension = parts.last()
        val stemParts = parts.dropLast(1)

        val alreadyPresent = stemParts.any { existing ->
            stuff.shortcuts.any { shortcut -> normalize(existing) == normalize(shortcut) }
        }
        if (alreadyPresent) return fileName

        return (stemParts + canonicalShortcut + extension).joinToString(".")
    }

    private fun normalize(value: String): String = value.trim().lowercase()
}
