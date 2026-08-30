package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.ActressName
import lorry.dossiertau.usecases.generateHTMLs.support.Subject

interface IWebScrappingRepo {

    suspend fun getWebPageActresses(name: TauItemName, localActresses: List<Actress>): Pair<MovieHtml, List<ActressName>>
    suspend fun getWebPageSubjects(
        name: TauItemName,
        movieHtml: MovieHtml,
        localSubjects: List<Subject>
    ): List<Subject>
}