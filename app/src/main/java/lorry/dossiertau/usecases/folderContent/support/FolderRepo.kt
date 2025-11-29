package lorry.dossiertau.usecases.folderContent.support

import android.util.Log
import androidx.compose.material3.rememberTooltipState
import arrow.core.Option
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.transfer.TauRepoFile
import lorry.dossiertau.data.transfer.TauRepoFolder
import lorry.dossiertau.data.transfer.TauRepoItem
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauFileName
import lorry.dossiertau.support.littleClasses.toTauPath
import java.io.File

class FolderRepo : IFolderRepo {
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
}


