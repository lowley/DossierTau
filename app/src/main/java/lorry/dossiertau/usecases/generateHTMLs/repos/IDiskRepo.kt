package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject

interface IDiskRepo {

    suspend fun getLocalActresses(): List<Actress>
    suspend fun getLocalSubjects(): List<Subject>
}