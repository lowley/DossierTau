package lorry.dossiertau.usecases.generateHTMLs.repos

import data.ftp.FtpDS
import lorry.dossiertau.data.model.name
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.toTauPath

class NasRepo(
    val ftpDS: FtpDS
): INasRepo {

    val rootPath = "/videos".toTauPath()

    override suspend fun renameFile(from: TauItemName, to: TauItemName){
        println("SCRAP \uD83D\uDE91 ${from.value} ▶ ${to.value}")
        ftpDS.rename(from, to, rootPath)
    }

    override suspend fun getVideoNames(): List<TauItemName> {
        val result = ftpDS.fetchVideoFiles(rootPath)?.map { it.name } ?: emptyList()
        return result
    }
}