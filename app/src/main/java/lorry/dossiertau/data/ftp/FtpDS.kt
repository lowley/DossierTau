package data.ftp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.name
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauDate
import lorry.dossiertau.support.littleClasses.toTauPath
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPClientConfig
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.time.LocalDate

open class FtpDS constructor() : IFtpDS {

    private suspend fun <T : Any?> doWithNASAccess(
        parent: String,
        doWithFtpClient: suspend (FTPClient) -> Result<T?>
    ): T? {

        val ftp: FTPClient = FTPClient()
        val config: FTPClientConfig = FTPClientConfig()
        config.setServerTimeZoneId("Europe/Paris")
        ftp.configure(config)

        var answer: T? = null

        try {
            val server = "192.168.1.20"
            ftp.connect(server)


            val reply = ftp.getReplyCode()
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect()
                println("FTP server refused connection.")
                throw Exception("FTP server refused connection.")
            }

            val connected = ftp.login("olivier", "37-2lematin")
            if (!connected) {
                println("Login failed")
                throw Exception("Login failed")
            }

            val result = doWithFtpClient(ftp)
            if (result.isSuccess)
                answer = result.getOrNull()
            else answer = null

            ftp.logout()
        } catch (ex: Exception) {
            println("erreur: ${ex.message}")
        } finally {
            if (ftp.isConnected) {
                try {
                    ftp.disconnect()
                } catch (ex: Exception) {
                }
            }

        }

        return answer
    }

    override suspend fun fetchVideoFiles(parent: TauPath): List<TauFile>? {
        return withContext(Dispatchers.IO) {
            doWithNASAccess(parent.path) { ftp ->
                Log.d("TEST", "TEST: inside")

                var result: List<TauFile> = listOf()
                val liste =
                    try {
                        Log.d("TEST", "TEST: parent=$parent")
                        Log.d("TEST", "TEST: ftp=$ftp")

                        val result1 = ftp.listFiles()
                        result = result1
                            ?.filter { file -> file.name.endsWith(".mp4") }
                            ?.map { videoFile ->
                                val date = try {
                                    LocalDate.of(
                                        videoFile.timestamp.time.year + 1900,
                                        videoFile.timestamp.time.month,
                                        videoFile.timestamp.time.date
                                    )
                                } catch (ex: Exception) {
                                    null
                                }
                                TauFile.of(
                                    fullPath = Paths.get(parent.path, videoFile.name).toString()
                                        .toTauPath(),
                                    picture = TauPicture.NONE,
                                    modificationDate = videoFile.timestamp.timeInMillis.toTauDate(),
                                    size = videoFile.size,
                                    fileId = FileId.EMPTY
                                )
                            } ?: listOf()

                        Log.d("TEST", "TEST: fichiers lus=${result.size}")

                    } catch (exception: Exception) {
                        println("Sigma: ${exception.message}")
                        result = listOf()
                    }

                Result.success(result)
            }
        }
    }

    override suspend fun fetchMP4File(parent: TauPath): List<TauFile>? {
        return doWithNASAccess(parent.path) { ftp ->
            val liste = withContext(Dispatchers.IO) {
                ftp.listFiles(parent.path)
                    ?.filter { file -> file.name.endsWith(".mp4") }
                    ?.map { file ->
                        TauFile.of(
                            fullPath = Paths.get(parent.path, file.name).toString().toTauPath(),
                            picture = TauPicture.NONE,
                            modificationDate = file.timestamp.timeInMillis.toTauDate(),
                            size = file.size,
                            fileId = FileId.EMPTY
                        )
                    }
            }

            Result.success(liste)
        }
    }

    override suspend fun copy(
        file: TauFile,
        pathOnNAS: TauPath,
        progressCallback: (Int) -> Unit // Ajout du callback pour la progression
    ): Boolean {
        return doWithNASAccess<Boolean>(parent = pathOnNAS.path) { ftp ->
            val localFilePath = file.fullPath
            val remoteFilePath = "$pathOnNAS/${file.name}"

            try {
                ftp.setFileType(FTPClient.BINARY_FILE_TYPE)

                val result = withContext(Dispatchers.IO) {
                    ftp.changeWorkingDirectory(pathOnNAS.path)
                }

                if (!result) {
                    println("Échec du changement de répertoire: $pathOnNAS")
                    return@doWithNASAccess Result.failure<Boolean>(Exception("Répertoire introuvable sur le NAS"))
                }

                val fileToUpload = java.io.File(localFilePath.path)
                val fileSize = fileToUpload.length() // Taille totale du fichier
                val buffer = ByteArray(4096) // Taille du buffer
                var uploadedSize = 0L

                fileToUpload.inputStream().use { inputStream ->
                    withContext(Dispatchers.IO) {
                        ftp.storeFileStream(file.name.value)?.use { outputStream ->
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                uploadedSize += bytesRead
                                val progress = (uploadedSize * 100 / fileSize).toInt()
                                progressCallback(progress) // Notifier la progression
                            }
                        }
                    }
                }

                if (ftp.completePendingCommand()) {
                    println("Fichier copié avec succès: $remoteFilePath")
                    Result.success(true)
                } else {
                    println("Échec de la copie du fichier: $remoteFilePath")
                    Result.failure(Exception("Échec de la copie"))
                }
            } catch (ex: Exception) {
                println("Erreur lors de la copie du fichier: ${ex.message}")
                Result.failure(ex)
            }
        } == true
    }

    override suspend fun rename(
        sourceName: TauItemName,
        destinationName: TauItemName,
        pathOnNAS: TauPath
    ): Boolean {
        return doWithNASAccess(parent = pathOnNAS.path) { ftp ->
            try {
                ftp.enterLocalPassiveMode()
                ftp.setFileType(FTPClient.BINARY_FILE_TYPE)

                // Se placer dans le dossier réel du fichier source
                val srcParent = sourceName.value.substringAfterLast("/", pathOnNAS.path)
                val srcBase = sourceName.value.substringAfterLast("/")
                val dstParent = destinationName.value.substringBeforeLast("/", srcParent)
                val dstBase = destinationName.value.substringAfterLast("/")

                // Aller dans le dossier source (plus fiable que forcer "/videos")
                val ok = withContext(Dispatchers.IO) { ftp.changeWorkingDirectory(srcParent) }
                if (!ok) return@doWithNASAccess Result.failure<Boolean>(IOException("Répertoire introuvable: $srcParent"))

                // Si on reste dans le même dossier, utiliser juste les noms
                val renameOk =
                    if (srcParent == dstParent) {
                        ftp.rename(srcBase, dstBase)
                    } else {
                        // Déplacement + renommage en un coup (chemin absolu côté destination)
                        ftp.rename(srcBase, (destinationName.value))
                    }

                if (renameOk) Result.success(true)
                else Result.failure<Boolean>(IOException("Échec du renommage $sourceName -> $destinationName"))
            } catch (ex: IOException) {
                Result.failure(ex)
            }
        } == true
    }

    override suspend fun copy(
        localFilePath: TauPath,
        pathOnNAS: TauPath,
        progressCallback: (Int) -> Unit // Ajout du callback pour la progression
    ): Boolean {
        return doWithNASAccess<Boolean>(parent = pathOnNAS.path) { ftp ->
            val remoteFilePath = "$pathOnNAS/${localFilePath.path.substringAfterLast("/")}"

            try {
                ftp.setFileType(FTPClient.BINARY_FILE_TYPE)

                val result = withContext(Dispatchers.IO) {
                    ftp.changeWorkingDirectory(pathOnNAS.path)
                }

                if (!result) {
                    println("Échec du changement de répertoire: $pathOnNAS")
                    return@doWithNASAccess Result.failure<Boolean>(Exception("Répertoire introuvable sur le NAS"))
                }

                val fileToUpload = java.io.File(localFilePath.path)
                val fileSize = fileToUpload.length() // Taille totale du fichier
                val buffer = ByteArray(4096) // Taille du buffer
                var uploadedSize = 0L

                fileToUpload.inputStream().use { inputStream ->
                    withContext(Dispatchers.IO) {
                        ftp.storeFileStream(localFilePath.path.substringAfterLast("/"))
                            ?.use { outputStream ->
                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    uploadedSize += bytesRead
                                    val progress = (uploadedSize * 100 / fileSize).toInt()
                                    progressCallback(progress) // Notifier la progression
                                }
                            }
                    }
                }

                if (ftp.completePendingCommand()) {
                    println("Fichier copié avec succès: $remoteFilePath")
                    Result.success(true)
                } else {
                    println("Échec de la copie du fichier: $remoteFilePath")
                    Result.failure(Exception("Échec de la copie"))
                }
            } catch (ex: Exception) {
                println("Erreur lors de la copie du fichier: ${ex.message}")
                Result.failure(ex)
            }
        } == true
    }

    override suspend fun delete(
        fileFullPath: TauPath
    ): Boolean {
        return doWithNASAccess<Boolean>(parent = fileFullPath.path) { ftp ->
            val success = withContext(Dispatchers.IO) {
                ftp.deleteFile(fileFullPath.path)
            }

            if (success)
                return@doWithNASAccess Result.success(true)
            else
                return@doWithNASAccess Result.failure(Exception())
        } == true
    }

    suspend fun createPath(path: TauPath): Boolean {
        return doWithNASAccess<Boolean>(parent = path.path) { ftp ->
            try {
                // Découper le chemin pour créer chaque dossier manquant un par un
                val directories = path.path.trim('/').split('/')
                var currentPath = ""

                for (dir in directories) {
                    currentPath += "/$dir"

                    // Vérifier si le répertoire existe déjà
                    if (!ftp.changeWorkingDirectory(currentPath)) {
                        val success = withContext(Dispatchers.IO) {
                            ftp.makeDirectory(currentPath)
                        }

                        if (!success) {
                            println("Échec de la création du répertoire : $currentPath")
                            return@doWithNASAccess Result.failure<Boolean>(Exception("Impossible de créer le répertoire $currentPath"))
                        } else {
                            println("Répertoire créé avec succès : $currentPath")
                        }
                    }
                }

                Result.success(true)
            } catch (ex: Exception) {
                println("Erreur lors de la création du répertoire : ${ex.message}")
                Result.failure(ex)
            }
        } == true
    }

    override suspend fun download(
        sourceFullPath: TauPath,
        localTargetFile: File,
        progressCallback: (Int) -> Unit
    ): Boolean {

        return doWithNASAccess<Boolean>(parent = sourceFullPath.path) { ftp ->
            try {
                ftp.setFileType(FTPClient.BINARY_FILE_TYPE)

                val remoteFile = ftp.mlistFile(sourceFullPath.path)
                val totalSize = remoteFile?.size ?: return@doWithNASAccess Result.failure(
                    Exception("Fichier introuvable")
                )

                val buffer = ByteArray(4096)
                var downloadedSize = 0L

                localTargetFile.outputStream().use { outputStream ->
                    withContext(Dispatchers.IO) {
                        ftp.retrieveFileStream(sourceFullPath.path)?.use { inputStream ->
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                downloadedSize += bytesRead
                                val progress = (downloadedSize * 100 / totalSize).toInt()
                                progressCallback(progress)
                            }
                        }
                    }
                }

                if (ftp.completePendingCommand()) {
                    println("✅ Fichier téléchargé avec succès : ${localTargetFile.absolutePath}")
                    Result.success(true)
                } else {
                    println("❌ Échec de la commande FTP après récupération.")
                    Result.failure(Exception("Commande FTP incomplète"))
                }

            } catch (ex: Exception) {
                println("❌ Erreur de téléchargement : ${ex.message}")
                Result.failure(ex)
            }
        } == true
    }
}
