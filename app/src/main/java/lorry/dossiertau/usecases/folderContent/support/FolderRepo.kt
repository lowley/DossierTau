package lorry.dossiertau.usecases.folderContent.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.dbModel.base64ToByteArray
import lorry.dossiertau.data.diskTransfer.TauRepoFile
import lorry.dossiertau.data.diskTransfer.TauRepoFolder
import lorry.dossiertau.data.diskTransfer.TauRepoItem
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.data.intelligenceService.utils2.events.SnapshotElement
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.intelligenceService.utils2.repo.ISpyRepo
import lorry.dossiertau.data.intelligenceService.utils2.repo.SpyRepo
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauFileName
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.support.littleClasses.toTauPicture
import lorry.dossiertau.ui.support.capsule.utilities.FileCapsuleManager
import lorry.dossiertau.ui.support.capsule.utilities.FolderCapsuleManager
import lorry.dossiertau.usecases.generateHTMLs.toBitmap
import java.io.File
import kotlin.let

open class FolderRepo(
    val spyRepo: ISpyRepo
) : IFolderRepo {
    private fun convertFileToTauRepoItem(file: File): TauRepoItem? {
        if (file.parent == null)
            return null

        val result = if (file.isFile)
            TauRepoFile(
                parentPath = file?.parent!!.toTauPath(),
                name = file.name.toTauFileName(),
                modificationDate = TauDate(file.lastModified()),
                fileId = spyRepo.getIdOf(file.path.toTauPath())
            )
        else TauRepoFolder(
            parentPath = file.parent!!.toTauPath(),
            name = file.name.toTauFileName(),
            modificationDate = TauDate(file.lastModified()),
            fileId = spyRepo.getIdOf(file.path.toTauPath())
        )

        return result
    }

    override suspend fun getItemsInFullPath(tauPath: TauPath): List<TauRepoItem> {

        val items = try {
            tauPath.toFile().fold(
                ifEmpty = { emptyList<File>() },
                ifSome = { file ->
                    val result = withContext(Dispatchers.IO) {
                        val files = file.listFiles()?.toList() ?: emptyList<File>()
                        files
                    }
                    result
                }
            ).map { file ->
                convertFileToTauRepoItem(file)
            }

        } catch (ex: SecurityException) {
            Log.d(
                "files",
                "SecurityException error in DiskDataSource/getFolderContent: ${ex.message}"
            )
            emptyList<TauRepoItem>()
        }.filterNotNull()

        return items
    }

    override suspend fun createSnapshotFor(folderPath: TauPath): Snapshot {
        val files = folderPath.toFile().getOrNull()?.listFiles().orEmpty()
        val content = files.associate { f ->

            val filePath = f.path
            val isFile = f.isFile
            val capsule = if (isFile) FileCapsuleManager(filePath, useOld = false).getCapsule()
                else FolderCapsuleManager(filePath.toTauPath(), useOld = false).getCapsule()

            val bitmap = capsule?.initialPicture?.let { base64ToByteArray(it).toBitmap() }
            val memo = capsule?.memo

            f.name to SnapshotElement(
                name = f.name,
                isDir = f.isDirectory,
                size = if (f.isFile) f.length() else 0L,
                lastModified = f.lastModified(),
                fileId = spyRepo.getIdOf(f.path.toTauPath()),
                picture = bitmap?.toTauPicture(),
                memo = memo,
            )
        }

        return Snapshot(
            folderPath = folderPath,
            entriesByName = content
        )
    }

    override suspend fun extractImageFromHtml(html: TauPath): Bitmap? {

        val htmlFile = html.toFile().getOrNull() ?: return null
        if (!withContext(Dispatchers.IO) { htmlFile.exists() }) return null

        val htmlContent = withContext(Dispatchers.IO) { htmlFile.readText() }

        // Regex pour trouver le contenu de src="data:image/...;base64,..."
        val regex = Regex("""<img\s+[^>]*src\s*=\s*"data:image/[^;]+;base64,([^"]+)"""")
        val match = regex.find(htmlContent) ?: return null

        val base64Image = match.groupValues[1]
        return try {
            withContext(Dispatchers.Default) {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val result = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                result
            }

        } catch (e: Exception) {
            println("Erreur lors du décodage de l'image : ${e.message}")
            null
        }
    }


}


