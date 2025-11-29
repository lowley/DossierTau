package lorry.dossiertau.usecases.folderContent

import android.app.job.JobScheduler
import arrow.core.None
import arrow.core.Option
import arrow.core.raise.fold
import arrow.core.toOption
import com.petertackage.kotlinoptions.optionOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.computeParentFolderDate
import lorry.dossiertau.data.transfer.toTauItems
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.name
import lorry.dossiertau.support.littleClasses.parentPath
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo

class FolderCompo(
    val folderRepo: IFolderRepo,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): IFolderCompo {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    //#[[folderFlowDeclaration]]
    private val _folderFlow = MutableStateFlow<Option<TauFolder>>(None)
    override val folderFlow = _folderFlow.asStateFlow()

    private fun changeFolderFlow(folder: Option<TauFolder>){
        println("DEBUG: changeFolderFlow($folder)")
        _folderFlow.update { folder }
    }

    /**
     * set folderFlow à "folder"
     */
    override fun setFolderFlow(folderFullPath: TauPath) {
        //#[[coroutine longue]]
        scope.launch {
            val repoItems = folderRepo.getItemsInFullPath(folderFullPath)
            val compoItems = repoItems.toTauItems()

            val folderDate = compoItems.computeParentFolderDate()

            val result = TauFolder(
                parentPath = folderFullPath.parentPath,
                name = folderFullPath.name,
                picture = TauPicture.NONE,
                modificationDate = folderDate,
                children = compoItems
            )
            println("DEBUG: result=$result")

            changeFolderFlow(result.toOption())
        }
    }

    override val folderPathFlow: StateFlow<Option<TauPath>>
        get() = folderFlow.map { it.fold(
            ifEmpty = { None },
            ifSome = { it.parentPath.toOption() }
        ) }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = Option.fromNullable(null)
        )
}