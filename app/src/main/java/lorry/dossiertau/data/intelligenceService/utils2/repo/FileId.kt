package lorry.dossiertau.data.intelligenceService.utils2.repo

import kotlinx.serialization.Serializable

/**
 * identifie un fichier, résistance au déplacement, renommage, modification
 */
@Serializable
sealed class FileId(){

    object EMPTY: FileId()
    @Serializable
    data class FileIdValue(val dev: Long, val ino: Long): FileId()

    companion object{
        open fun fileIdOf(dev: Long, ino: Long) = FileIdValue(dev, ino)
    }

    inline val id: FileIdValue?
        get() = (this as? FileIdValue) ?: null
}

