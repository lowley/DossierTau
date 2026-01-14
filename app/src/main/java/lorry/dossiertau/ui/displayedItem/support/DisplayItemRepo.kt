package lorry.dossiertau.ui.displayedItem.support

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.name
import java.io.File

class DisplayItemRepo {

    suspend fun getInfoSup(item: TauItem): String {
        return withContext(Dispatchers.IO) {
            val infos = if (item is TauFolder)
                countFilesAndFolders(item.fullPath.toFile().getOrNull() ?: return@withContext "").component1().toString() else item.name.value
                .substringAfterLast(".").toUpperCase(Locale.current)

            infos
        }
    }

    suspend fun getInfoInf(item: TauItem): String {
        return withContext(Dispatchers.IO) {
            val infos = if (item is TauFolder)
                countFilesAndFolders(item.fullPath.toFile().getOrNull() ?: return@withContext "").component2()
                    .toString()
            else formatFileSizeShort(getSize(item.fullPath.toFile().getOrNull() ?: return@withContext ""))

            infos
        }
    }

    suspend fun countFilesAndFolders(folder: File): Pair<Int, Int> {
        if (!folder.isDirectory) return 0 to 0

        val files = folder.listFiles() ?: return 0 to 0
        var fileCount = 0
        var folderCount = 0

        for (f in files) {
            if (
                f.isFile &&
                !f.name.startsWith(".")
            ) fileCount++
            else if (f.isDirectory) if (
                !f.name.startsWith(".")
            )
                folderCount++
        }

        return fileCount to folderCount
    }

    private fun formatFileSizeShort(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        val z = (63 - java.lang.Long.numberOfLeadingZeros(bytes)) / 10
        val value = bytes.toDouble() / (1L shl (z * 10))
        return String.format("%.1f%c", value, " KMGTPE"[z])
    }

    suspend fun getSize(file: File): Long {
        return withContext(Dispatchers.IO) {
            val result = file.length()
            result
        }
    }

}