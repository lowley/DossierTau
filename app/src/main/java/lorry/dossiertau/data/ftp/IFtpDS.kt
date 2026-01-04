package data.ftp

import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import java.io.File

interface IFtpDS {
    suspend fun fetchVideoFiles(parent: TauPath): List<TauFile>?
    suspend fun fetchMP4File(parent: TauPath): List<TauFile>?
    suspend fun copy(file: TauFile, pathOnNAS: TauPath,
                     progressCallback: (Int) -> Unit): Boolean
    suspend fun rename(
        sourceName: TauItemName,
        destinationName: TauItemName,
        pathOnNAS: TauPath
    ): Boolean

    suspend fun copy(localFilePath: TauPath, pathOnNAS: TauPath,
        progressCallback: (Int) -> Unit): Boolean
    
    suspend fun delete(fileFullPath: TauPath): Boolean
    suspend fun download(
        sourceFullPath: TauPath,
        localTargetFile: File,
        progressCallback: (Int) -> Unit
    ): Boolean

    suspend fun createDescriptionFileInAnnexes(
        fileName: TauItemName,
        textContent: String
    ): Boolean

    suspend fun createPictureFileInAnnexes(fileName: TauItemName, url: String): Boolean
}