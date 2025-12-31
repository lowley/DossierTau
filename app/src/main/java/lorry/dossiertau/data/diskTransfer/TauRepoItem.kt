package lorry.dossiertau.data.diskTransfer

import kotlinx.serialization.Serializable
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath

@Serializable
sealed interface TauRepoItem {
    val id: TauIdentifier
    val parentPath: TauPath
    val name: TauItemName
    val modificationDate: TauDate
    val fileId: FileId
}

fun Collection<TauRepoItem>.files() = this.filter { it is TauRepoFile }
fun Collection<TauRepoItem>.folders() = this.filter { it is TauRepoFolder }

fun Collection<TauRepoItem>.toTauItems(): List<TauItem> {
    val result = this.map {
        when (it){
            is TauRepoFile -> it.toTauFile()
            is TauRepoFolder -> it.toTauFolder()
        }
    }

    return result
}