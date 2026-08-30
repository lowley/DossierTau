package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject

interface IDiskRepo {

    suspend fun getVideoActresses(): List<Actress>
    suspend fun getVideoSubjects(): List<Subject>
    suspend fun deleteAllHtmlsIn(root: TauPath)
}