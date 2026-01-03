package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject

interface IWebScrappingRepo {

    suspend fun getMovieActresses(name: TauItemName): List<String>
    suspend fun getMovieSubjects(name: TauItemName): List<String>
}