package lorry.dossiertau.data.transfer

import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture

data class TauRepoFolder (
    override val id: TauIdentifier = TauIdentifier.random(),
    override val parentPath: TauPath = TauPath.EMPTY,
    override val name: TauItemName = TauItemName.EMPTY,
    override val modificationDate: TauDate = TauDate.now(),




): TauRepoItem {

    constructor(
        fullPath: TauPath,
        picture: TauPicture = TauPicture.NONE,
        modificationDate: TauDate = TauDate.now(),
        children: List<TauItem> = emptyList(),
        id: TauIdentifier = TauIdentifier.random(),
    ) : this(
        parentPath = splitParentAndName(fullPath).first,
        name = splitParentAndName(fullPath).second,
        modificationDate = modificationDate,
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

fun TauRepoFolder.toTauFolder(): TauFolder{
    return TauFolder(
        id = this.id,
        parentPath = this.parentPath,
        name = this.name,
        picture = TauPicture.NONE,
        modificationDate = this.modificationDate,
        children = emptyList(),
    )
}