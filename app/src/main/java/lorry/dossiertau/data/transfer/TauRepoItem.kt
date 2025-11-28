package lorry.dossiertau.data.transfer

import androidx.compose.foundation.text.input.rememberTextFieldState
import kotlinx.serialization.Serializable
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