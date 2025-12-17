package lorry.dossiertau.usecases.folderContent

import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.data.dbModel.OpType
import lorry.dossiertau.data.dbModel.toTauItem
import lorry.dossiertau.data.model.computeParentFolderDate
import lorry.dossiertau.data.diskTransfer.toTauItems
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.parentPath
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.name
import lorry.dossiertau.support.littleClasses.parentPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo

class FolderCompo(
    val folderRepo: IFolderRepo,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fileDiffDAO: FileDiffDao
) : IFolderCompo {

    companion object {
        var collectFillLaunched = false
    }

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    //#[[folderFlowDeclaration]]
    private val _folderFlow = MutableStateFlow<Option<TauFolder>>(None)
    override val folderFlow = _folderFlow.asStateFlow()

    private fun changeFolderFlow(folder: Option<TauFolder>) {
        println("DEBUG: changeFolderFlow: ${folder.display()}")
        _folderFlow.update { folder }
    }

    /**
     * set folderFlow à "folder"
     */
    override fun setFolderFlow(folderFullPath: TauPath) {
        //#[[coroutine longue]]
        scope.launch(dispatcher) {
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

            println("DEBUG: setFolderFlow:${result.fullPath}")
            val res2 = result.toOption()
            changeFolderFlow(res2)
        }
    }

    override val folderPathFlow: StateFlow<Option<TauPath>>
        get() = folderFlow.map {
            it.fold(
                ifEmpty = { None },
                ifSome = { it.fullPath.toOption() }
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = Option.fromNullable(null)
        )

    init {
        if (!collectFillLaunched) {
            collectFillLaunched = true
            scope.launch(dispatcher) {
                collectDiffs()
            }
        }
    }

    private suspend fun collectDiffs() {
        combineTransform(fileDiffDAO.diffFlow().filterNotNull(), folderPathFlow){ diff, path ->
            when (diff.op_type){
                OpType.FolderRefresh.text -> {
                    if (diff.full_path == path.getOrNull()?.path)
                        emit(diff)
                }

                else -> {
                    if (diff.parentPath == path.getOrNull()?.path)
                        emit(diff)
                }
            }
        }.collect { diff ->
                println("COLLECTDIFFS: reçu diff: $diff")
                val folder = folderFlow.value.getOrNull() ?: return@collect

                //TODO tester si children contient déjà item
                println("DEBUG: reçu un diff, type = ${diff.op_type}")

                if (diff.op_type == OpType.CreateItem.text)
                    changeFolderFlow(folder.addItem(diff.toTauItem()).toOption())

                if (diff.op_type == OpType.DeleteItem.text) {
                    println("rentre dans removeItem, avec TauItem=${diff}")
                    changeFolderFlow(folder.removeItem(diff.toTauItem()).toOption())
                }

                if (diff.op_type == OpType.ModifyItem.text)
                    changeFolderFlow(folder.modifyItem(diff.toTauItem()).toOption())
            }
    }
}

fun Option<TauFolder>.display(): String {

    val PB = "\uD835\uDED5Folder(PB)"
    val NONE = "\uD835\uDED5Folder(NONE)"

    val isNull = this.getOrNull() == null
    if (isNull)
        return NONE

    val parentPath = this.getOrNull()?.parentPath?.toString() ?: return PB

    val data = this.getOrNull()?.toString() ?: NONE
    return data
}

