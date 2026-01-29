package lorry.dossiertau.ui.support.capsule.utilities
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path

class FolderCapsuleManager(
    private val targetPath: TauPath,
    private val useOld: Boolean = false
) {
    suspend fun save(element: IElementInCapsule) {
        val targetHtmlPath = targetPath.appendToTauPath(".folderPicture.html").path
        FileCapsuleManager(targetHtmlPath, false).save(element, forFolder = true)
    }

    suspend fun getCapsule(loadBitmaps: Boolean = true): CapsuleData? {
        val targetHtmlPath = targetPath.appendToTauPath(".folderPicture.html").path

        return FileCapsuleManager(targetHtmlPath, false).getCapsule(loadBitmaps)
    }

    suspend fun getFolderBitmap(): Bitmap? {
        val targetHtmlPath = targetPath.appendToTauPath(".folderPicture.html").path
        return FileCapsuleManager(targetHtmlPath, false).getFolderBitmap()
    }

    suspend fun <T> getElement(reader: IElementReader<T>): T? {
        return withContext(Dispatchers.IO) {
            reader.fileGet(targetPath)
        }
    }
}