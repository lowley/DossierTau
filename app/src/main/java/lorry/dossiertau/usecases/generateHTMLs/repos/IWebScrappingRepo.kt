package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject

interface IWebScrappingRepo {

    fun getMovieActresses(name: TauItemName): List<Actress>
    fun getMovieSubjects(name: TauItemName): List<Subject>
}