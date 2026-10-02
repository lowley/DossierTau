package lorry.dossiertau.usecases.folderContent.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.dbModel.base64ToByteArray
import lorry.dossiertau.data.diskTransfer.TauRepoFile
import lorry.dossiertau.data.diskTransfer.TauRepoFolder
import lorry.dossiertau.data.diskTransfer.TauRepoItem
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.data.intelligenceService.utils2.events.SnapshotElement
import lorry.dossiertau.data.intelligenceService.utils2.repo.ISpyRepo
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.name
import lorry.dossiertau.support.littleClasses.toTauFileName
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.support.littleClasses.toTauPicture
import lorry.dossiertau.ui.support.capsule.utilities.FileCapsuleManager
import lorry.dossiertau.ui.support.capsule.utilities.FolderCapsuleManager
import lorry.dossiertau.usecases.generateHTMLs.toBitmap
import java.io.File

open class FolderRepo(
    val spyRepo: ISpyRepo
) : IFolderRepo {
    private fun convertFileToTauRepoItem(file: File): TauRepoItem? {
        if (file.parent == null) return null

        return if (file.isFile)
            TauRepoFile(
                parentPath = file.parent!!.toTauPath(),
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
    }

    override suspend fun getItemsInFullPath(tauPath: TauPath): List<TauRepoItem> {
        val items = try {
            tauPath.toFile().fold(
                ifEmpty = { emptyList<File>() },
                ifSome = { file ->
                    withContext(Dispatchers.IO) {
                        file.listFiles()?.toList() ?: emptyList()
                    }
                }
            ).map { file -> convertFileToTauRepoItem(file) }
        } catch (ex: SecurityException) {
            Log.d("files", "SecurityException error in DiskDataSource/getFolderContent: ${ex.message}")
            emptyList<TauRepoItem>()
        }.filterNotNull()

        return items
    }

    // Spy utilise ce chemin : le snapshot doit rester très bon marché.
    // Les images sont enrichies ensuite, à la demande, par loadThumbnail().
    override suspend fun createSnapshotFor(folderPath: TauPath): Snapshot =
        createStructuralSnapshotFor(folderPath)

    override suspend fun createStructuralSnapshotFor(folderPath: TauPath): Snapshot =
        withContext(Dispatchers.IO) {
            val files = folderPath.toFile().getOrNull()?.listFiles().orEmpty()
            Snapshot(
                folderPath = folderPath,
                entriesByName = files.associate { file ->
                    file.name to structuralSnapshotElement(file)
                }
            )
        }

    private fun structuralSnapshotElement(file: File) = SnapshotElement(
        name = file.name,
        isDir = file.isDirectory,
        size = if (file.isFile) file.length() else 0L,
        lastModified = file.lastModified(),
        fileId = spyRepo.getIdOf(file.path.toTauPath()),
        picture = null,
        memo = null,
    )

    private fun decodeThumbnail(bytes: ByteArray, targetPx: Int = 320): Bitmap? {
        if (bytes.isEmpty()) return null

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / (sampleSize * 2) >= targetPx &&
            bounds.outHeight / (sampleSize * 2) >= targetPx
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun scaleThumbnail(bitmap: Bitmap, targetPx: Int = 320): Bitmap {
        if (bitmap.width <= targetPx && bitmap.height <= targetPx) return bitmap

        val ratio = minOf(
            targetPx.toFloat() / bitmap.width,
            targetPx.toFloat() / bitmap.height,
        )

        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)

        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun decodeBase64Thumbnail(base64: String, targetPx: Int = 320): Bitmap? =
        runCatching {
            decodeThumbnail(base64ToByteArray(base64), targetPx)
        }.getOrNull()

    private suspend fun extractThumbnailFromHtml(
        html: TauPath,
        targetPx: Int = 320,
    ): Bitmap? {
        val htmlFile = html.toFile().getOrNull() ?: return null
        if (!withContext(Dispatchers.IO) { htmlFile.exists() }) return null

        val htmlContent = withContext(Dispatchers.IO) { htmlFile.readText() }
        val regex = Regex("""<img\s+[^>]*src\s*=\s*"data:image/[^;]+;base64,([^"]+)"""")
        val match = regex.find(htmlContent) ?: return null

        return withContext(Dispatchers.Default) {
            runCatching {
                val imageBytes = Base64.decode(match.groupValues[1], Base64.DEFAULT)
                decodeThumbnail(imageBytes, targetPx)
            }.getOrNull()
        }
    }

    private suspend fun loadCapsuleData(file: File): Pair<Bitmap?, String?> {
        val filePath = file.path
        return if (file.isFile) {
            val capsule = FileCapsuleManager(filePath, useOld = false).getCapsule()
            var bitmap = capsule.initialPicture?.let { base64ToByteArray(it).toBitmap() }
            if (bitmap == null && file.extension.lowercase() == "html") {
                bitmap = extractImageFromHtml(filePath.toTauPath())
            }
            bitmap to capsule.memo
        } else {
            val manager = FolderCapsuleManager(filePath.toTauPath(), useOld = false)
            val bitmap = manager.getFolderBitmap()
            if (bitmap != null) {
                val capsule = manager.getCapsule(loadBitmaps = false)
                bitmap to capsule?.memo
            } else {
                val capsule = manager.getCapsule(loadBitmaps = true)
                val fallback = capsule?.initialPicture?.let { base64ToByteArray(it).toBitmap() }
                fallback to capsule?.memo
            }
        }
    }

    override suspend fun loadThumbnail(itemPath: TauPath): TauPicture? = withContext(Dispatchers.IO) {
        val file = itemPath.toFile().getOrNull() ?: return@withContext null
        if (!file.exists()) return@withContext null

        val targetPx = 320

        val bitmap = if (file.isFile) {
            val capsule = FileCapsuleManager(file.path, useOld = false).getCapsule()
            capsule.initialPicture
                ?.let { decodeBase64Thumbnail(it, targetPx) }
                ?: if (file.extension.lowercase() == "html") {
                    extractThumbnailFromHtml(itemPath, targetPx)
                } else {
                    null
                }
        } else {
            val manager = FolderCapsuleManager(itemPath, useOld = false)
            manager.getFolderBitmap()
                ?.let { scaleThumbnail(it, targetPx) }
                ?: manager.getCapsule(loadBitmaps = true)
                    ?.initialPicture
                    ?.let { decodeBase64Thumbnail(it, targetPx) }
        }

        bitmap?.toTauPicture()
    }

    // Conservé pour les usages qui auraient besoin d'un snapshot entièrement enrichi.
    suspend fun getSnapshot(files: List<File>): Map<String, SnapshotElement> = coroutineScope {
        files.map { file ->
            async(Dispatchers.Default) {
                val (bitmap, memo) = loadCapsuleData(file)
                file.name to structuralSnapshotElement(file).copy(
                    picture = bitmap?.toTauPicture(),
                    memo = memo,
                )
            }
        }.awaitAll().toMap()
    }

    override suspend fun extractImageFromHtml(html: TauPath): Bitmap? {
        val htmlFile = html.toFile().getOrNull() ?: return null
        if (!withContext(Dispatchers.IO) { htmlFile.exists() }) return null
        val htmlContent = withContext(Dispatchers.IO) { htmlFile.readText() }
        val regex = Regex("""<img\s+[^>]*src\s*=\s*"data:image/[^;]+;base64,([^"]+)"""")
        val match = regex.find(htmlContent) ?: return null
        val base64Image = match.groupValues[1]
        return try {
            withContext(Dispatchers.Default) {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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
        println("Groupe: ${group.javaClass.simpleName}, temps total: ${group.time}ms")
    }
}

suspend fun <T> Groups.addTimeOf(block: suspend () -> T): T {
    val start = System.currentTimeMillis()
    val result = block()
    val time = System.currentTimeMillis() - start
    this.time += time
    return result
}
