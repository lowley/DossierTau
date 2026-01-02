package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauItemName

interface INasRepo {

    suspend fun renameFile(from: TauItemName, to: TauItemName)
    suspend fun getVideoNames(): List<TauItemName>
}