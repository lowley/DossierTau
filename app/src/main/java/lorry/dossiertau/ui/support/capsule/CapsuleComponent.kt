package lorry.dossiertau.ui.support.capsule

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.ui.support.capsule.utilities.CapsuleData
import lorry.dossiertau.ui.support.capsule.utilities.FileCapsuleManager
import lorry.dossiertau.ui.support.capsule.utilities.FolderCapsuleManager
import lorry.dossiertau.ui.support.capsule.utilities.IElementInCapsule
import lorry.dossiertau.ui.support.capsule.utilities.IElementReader

class CapsuleComponent: ICapsuleComponent {

    override suspend fun save(
        element: IElementInCapsule,
        targetPath: TauPath,
        useOld: Boolean
    ) {
        val file = targetPath.toFile()?.getOrNull()
        if (file?.exists() != true)
            return

        if (file.isFile())
            FileCapsuleManager(targetPath.path, useOld).save(element, forFolder = false)
        else {
            FolderCapsuleManager(targetPath, useOld).save(element)
        }
    }

    override suspend fun getCapsule(
        targetPath: TauPath,
        useOld: Boolean
    ): CapsuleData? {
        val file = targetPath.toFile().getOrNull()
        if (file?.exists() != true)
            return null

        //file ne sera pas null
        val result = if (file.isFile) {
            FileCapsuleManager(targetPath.path, useOld).getCapsule()
        } else {
            FolderCapsuleManager(targetPath, useOld).getCapsule()
        }

        return result
    }

    /**
     * lecture à chaque fois de l'info dans le fichier/dossier
     */
    override suspend fun <T> getElement(
        reader: IElementReader<T>,
        targetPath: TauPath
        ): T? {
        return withContext(Dispatchers.IO) {
            val file = targetPath.toFile().getOrNull()
            if (file?.exists() != true)
                return@withContext null

            return@withContext if (file.isFile)
                reader.fileGet(targetPath)
            else
                reader.folderGet(targetPath)
        }
    }
}