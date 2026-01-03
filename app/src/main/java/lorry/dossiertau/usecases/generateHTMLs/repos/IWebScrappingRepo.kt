package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.ActressName

interface IWebScrappingRepo {

    suspend fun getMovieActresses(name: TauItemName, localActresses: List<Actress>): Pair<MovieHtml, List<ActressName>>
    suspend fun getMovieSubjects(name: TauItemName, movieHtml: MovieHtml): List<String>
}