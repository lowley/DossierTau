package lorry.dossiertau.ui.support.capsule.utilities

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path

class FolderCapsuleManager(
    private val targetPath: TauPath,
    private val useOld: Boolean = false
) {
    suspend fun save(element: IElementInCapsule) {
        val targetHtmlPath = targetPath.path
            .plus(".folderPicture.html")
        FileCapsuleManager(targetHtmlPath, false).save(element, forFolder = true)
    }

    suspend fun getCapsule(): CapsuleData? {
        val targetHtmlPath = targetPath.path
            .plus("/.folderPicture.html")

        return FileCapsuleManager(targetHtmlPath, false).getCapsule()
    }

    suspend fun <T> getElement(reader: IElementReader<T>): T? {
        return withContext(Dispatchers.IO) {
            reader.fileGet(targetPath)
        }
    }
}