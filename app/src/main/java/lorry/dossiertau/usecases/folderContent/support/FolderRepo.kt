package lorry.dossiertau.usecases.folderContent.support

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.diskTransfer.TauRepoFile
import lorry.dossiertau.data.diskTransfer.TauRepoFolder
import lorry.dossiertau.data.diskTransfer.TauRepoItem
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.data.intelligenceService.utils2.events.SnapshotElement
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.intelligenceService.utils2.repo.ISpyRepo
import lorry.dossiertau.data.intelligenceService.utils2.repo.SpyRepo
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauFileName
import lorry.dossiertau.support.littleClasses.toTauPath
import java.io.File

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
                modificationDate = TauDate(file.lastModified())
            )
        else TauRepoFolder(
            parentPath = file.parent!!.toTauPath(),
            name = file.name.toTauFileName(),
            modificationDate = TauDate(file.lastModified()),
        )

        return result
    }

    override suspend fun getItemsInFullPath(tauPath: TauPath): List<TauRepoItem> {

            val items = try {
                tauPath.toFile().fold(
                    ifEmpty = { emptyList<File>() },
                    ifSome = { file ->
                        val result = withContext(Dispatchers.IO){
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
            f.name to SnapshotElement(
                name = f.name,
                isDir = f.isDirectory,
                size = if (f.isFile) f.length() else 0L,
                lastModified = f.lastModified(),
                fileId = spyRepo.getIdOf(f.path.toTauPath())
            )
        }

        return Snapshot(
            folderPath = folderPath,
            entriesByName = content
        )
    }
}


