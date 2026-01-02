package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject

class WebScrappingRepo: IWebScrappingRepo {

    override fun getMovieActresses(name: TauItemName): List<Actress>{
        return emptyList()



    }

    override fun getMovieSubjects(name: TauItemName): List<Subject>{
        return emptyList()



    }


}