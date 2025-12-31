package lorry.dossiertau.data.diskTransfer

import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauExtension
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.extension

data class TauRepoFile(
    override val id: TauIdentifier = TauIdentifier.random(),
    override val parentPath: TauPath = TauPath.EMPTY,
    override val name: TauItemName = TauItemName.EMPTY,
    override val modificationDate: TauDate = TauDate.now(),
    override val fileId: FileId,
    ) : TauRepoItem {

    val extension: TauExtension
        get() = name.extension


}

fun TauRepoFile.toTauFile(): TauFile{
    return TauFile(
        id = this.id,
        parentPath = this.parentPath,
        name = this.name,
        picture = TauPicture.NONE,
        modificationDate = this.modificationDate,
        fileId = this.fileId
    )
}