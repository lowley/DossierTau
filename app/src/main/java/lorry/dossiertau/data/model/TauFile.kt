package lorry.dossiertau.data.model

import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.model.TauFolder.Data
import lorry.dossiertau.data.planes.DbItem
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauExtension
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.extension
import kotlin.Long

sealed class TauFile private constructor() : TauItem {
    inline val asData: TauFile.Data? get() = this as? TauFile.Data

    data object EMPTY : TauFile()
    data class Data(
        override val id: TauIdentifier = TauIdentifier.random(),
        override val parentPath: TauPath,
        override val name: TauItemName,
        override val picture: TauPicture = TauPicture.NONE,
        override val modificationDate: TauDate = TauDate.now(),

        val size: Long = 0L
    ) : TauFile(), TauDataCommon {

        constructor(
            id: TauIdentifier = TauIdentifier.random(),
            fullPath: TauPath,
            picture: TauPicture = TauPicture.NONE,
            modificationDate: TauDate = TauDate.now(),
            size: Long = 0L
        ) : this(
            id = id,
            parentPath = splitParentAndName(fullPath).first,
            name = splitParentAndName(fullPath).second,
            picture = picture,
            modificationDate = modificationDate,
            size = size
        )
    }

    companion object {
        operator fun invoke(
            id: TauIdentifier = TauIdentifier.random(),
            fullPath: TauPath,
            picture: TauPicture = TauPicture.NONE,
            modificationDate: TauDate = TauDate.now(),
            size: Long = 0L
        ): TauFile = Data(
            id = id,
            parentPath = splitParentAndName(fullPath).first,
            name = splitParentAndName(fullPath).second,
            picture = picture,
            modificationDate = modificationDate,
            size = size
        )

        operator fun invoke(
            id: TauIdentifier = TauIdentifier.random(),
            parentPath: TauPath,
            name: TauItemName,
            picture: TauPicture = TauPicture.NONE,
            modificationDate: TauDate = TauDate.now(),
            size: Long = 0L
        ): TauFile = Data(id, parentPath, name, picture, modificationDate, size)

        private fun splitParentAndName(full: TauPath): Pair<TauPath, TauItemName> {
            val s = full.toString()
            val i = s.lastIndexOf('/')
            val parent = if (i <= 0) TauPath.EMPTY else TauPath.of(s.take(i))
            val base = if (i < 0) s else s.substring(i + 1)
            return parent to TauItemName(base)
        }
    }
}

inline val TauFile.name: TauItemName get() = asData?.name ?: TauItemName.EMPTY
inline val TauFile.parentPath: TauPath get() = asData?.parentPath ?: TauPath.EMPTY
inline val TauFile.picture: TauPicture get() = asData?.picture ?: TauPicture.NONE
inline val TauFile.modificationDate: TauDate
    get() = asData?.modificationDate ?: TauDate.fromLong(0L)

inline val TauFile.size: Long get() = asData?.size ?: 0L

fun TauFile.toDbFile() = DbItem(
    fullPath = this.fullPath,
    modificationDate = this.modificationDate,
    type = ItemType.FILE
)