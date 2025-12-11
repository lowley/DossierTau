package lorry.dossiertau.usecases.folderContent

import androidx.compose.material3.rememberTooltipState
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.dbModel.DiffEntity
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.data.dbModel.toTauItem
import lorry.dossiertau.data.model.computeParentFolderDate
import lorry.dossiertau.data.diskTransfer.toTauItems
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.children
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
            println("DEBUG: setFolderFlow:$result")

            changeFolderFlow(result.toOption())
        }
    }

    override val folderPathFlow: StateFlow<Option<TauPath>>
        get() = folderFlow.map {
            it.fold(
                ifEmpty = { None },
                ifSome = { it.parentPath.toOption() }
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
        folderPathFlow.flatMapLatest {
            if (it.isSome())
            // fileDiffDAO.diffsForFolder(path) est un Flow qui émet chaque fois que la DB change
                fileDiffDAO.diffsForFolder(it.getOrNull()!!.path)
            // Si le chemin est vide, on retourne un Flow qui n'émet rien mais NE SE TERMINE PAS
            // (sinon le collect se termine), ou flowOf(emptyList()) si on veut une valeur initiale.
            // Utilisateur: flowOf() semble suffisant s'il est vide, mais faisons-le propre.
            // Si le DAO retourne List<DiffEntity>, on retourne un Flow<List<DiffEntity>> vide.
            else flow<List<DiffEntity>> { kotlinx.coroutines.awaitCancellation() }
        }
            .collect { diff ->
                val folder = folderFlow.value.getOrNull() ?: return@collect

                //TODO tester si children contient déjà item
                println("DEBUG: reçu un diff, ${diff.size} éléments")
                println("       -> folderCompo:${this.dispatcher.toString().takeLast(5)}")
                changeFolderFlow(
                    //TODO compléter: plusieurs éléments dans diff
                    folder.addItem(diff[0].toTauItem()).toOption()
                )
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

