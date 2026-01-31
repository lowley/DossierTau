package lorry.dossiertau.usecases.folderContent.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import lorry.dossiertau.support.littleClasses.name
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
        val files = Groups.LISTFILES.addTimeOf {
            folderPath.toFile().getOrNull()?.listFiles().orEmpty()
        }.toList()

        val content = Groups.CAPSULE.addTimeOf {
            getSnapshot(files)
        }

        displayAllTimes(folderPath)

        return Snapshot(
            folderPath = folderPath,
            entriesByName = content
        )
    }

    suspend fun getSnapshot(files: List<File>): Map<String, SnapshotElement> = coroutineScope {
        files.map { f ->
            // On lance chaque traitement dans une coroutine séparée
            async(Dispatchers.Default) {
                val filePath = f.path
                val isFile = f.isFile

                val (bitmap, memo) = if (isFile) {
                    val capsule = FileCapsuleManager(filePath, useOld = false).getCapsule()
                    var b = capsule.initialPicture?.let { base64ToByteArray(it).toBitmap() }
                    if (b == null && f.extension.lowercase() == "html") {
                        b = extractImageFromHtml(filePath.toTauPath())
                    }
                    b to capsule.memo
                } else {
                    val fcm = FolderCapsuleManager(filePath.toTauPath(), useOld = false)
                    // Optimisation: pour les dossiers, on récupère le bitmap directement si possible
                    val b = fcm.getFolderBitmap()
                    if (b != null) {
                        // On a déjà l'image, on récupère juste le mémo sans charger les bitmaps du HTML
                        val capsule = fcm.getCapsule(loadBitmaps = false)
                        b to capsule?.memo
                    } else {
                        // Fallback si pas d'image directe
                        val capsule = fcm.getCapsule(loadBitmaps = true)
                        val b2 = capsule?.initialPicture?.let { base64ToByteArray(it).toBitmap() }
                        b2 to capsule?.memo
                    }
                }

                f.name to SnapshotElement(
                    name = f.name,
                    isDir = f.isDirectory,
                    size = if (isFile) f.length() else 0L,
                    lastModified = f.lastModified(),
                    fileId = spyRepo.getIdOf(f.path.toTauPath()),
                    picture = bitmap?.toTauPicture(),
                    memo = memo,
                )
            }
        }
            .awaitAll() // On attend que tout le monde ait fini
            .toMap()    // On convertit la liste de paires en Map
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

sealed class Groups(var time: Long) {
    object LISTFILES : Groups(0L)
    object CAPSULE : Groups(0L)
}

fun displayAllTimes(path: TauPath) {
    println("***")
    println("Groupe - ${path.name}")

    listOf(Groups.LISTFILES, Groups.CAPSULE).forEach { group ->
        val ligne = "Groupe: ${group.javaClass.simpleName}, temps total: ${group.time}ms"
        println(ligne)
    }
}

suspend fun <T> Groups.addTimeOf(block: suspend () -> T): T {

    val start = System.currentTimeMillis()
    val result = block()
    val time = System.currentTimeMillis() - start
    this.time += time
    return result
}
















