package lorry.dossiertau.usecases.generateHTMLs.repos

import lorry.dossiertau.support.littleClasses.TauItemName

class NasRepo: INasRepo {

    override suspend fun renameFile(from: TauItemName, to: TauItemName){



    }

    override fun getVideoPaths(): List<TauItemName> {
        return emptyList()
    }


}