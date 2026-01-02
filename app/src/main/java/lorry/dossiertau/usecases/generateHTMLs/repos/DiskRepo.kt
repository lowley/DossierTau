package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject

class DiskRepo: IDiskRepo {

    override suspend fun getLocalActresses(): List<Actress>{
        return emptyList()


    }

    override suspend fun getLocalSubjects(): List<Subject>{
        return emptyList()


    }


}