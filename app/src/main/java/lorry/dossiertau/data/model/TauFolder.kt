package lorry.dossiertau.data.model

import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture

data class TauFolder(
    override val id: TauIdentifier = TauIdentifier.random(),
    override val parentPath: TauPath = TauPath.EMPTY,
    override val name: TauItemName = TauItemName.EMPTY,
    override val picture: TauPicture = TauPicture.NONE,
    override val modificationDate: TauDate = TauDate.now(),

    val children: List<TauItem> = emptyList()


) : TauItem {

    constructor(
        fullPath: TauPath,
        picture: TauPicture = TauPicture.NONE,
        modificationDate: TauDate = TauDate.now(),
        children: List<TauItem> = emptyList(),
        id: TauIdentifier = TauIdentifier.random(),
    ) : this(
        parentPath = splitParentAndName(fullPath).first,
        name = splitParentAndName(fullPath).second,
        picture = picture,
        modificationDate = modificationDate,
        children = children,
        id = id,
    )

    companion object {
        private fun splitParentAndName(full: TauPath): Pair<TauPath, TauItemName> {
            val s = full.toString()
            val i = s.lastIndexOf('/')
            val parent = if (i <= 0) TauPath.EMPTY else TauPath(s.take(i))
            val base = if (i < 0) s else s.substring(i + 1)
            return parent to TauItemName(base)
        }
    }
}

