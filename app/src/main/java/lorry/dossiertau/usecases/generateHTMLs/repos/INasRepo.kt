package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauItemName

interface INasRepo {

    suspend fun renameFile(from: TauItemName, to: TauItemName)
    fun getVideoPaths(): List<TauItemName>
}