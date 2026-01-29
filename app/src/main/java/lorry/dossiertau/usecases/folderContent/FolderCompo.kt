package lorry.dossiertau.usecases.folderContent

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.dbModel.ContentWithItems
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.data.dbModel.OpType
import lorry.dossiertau.data.dbModel.TauEntity
import lorry.dossiertau.data.dbModel.toTauItem
import lorry.dossiertau.data.intelligenceService.ISpy
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.parentPath
import lorry.dossiertau.data.model.children
import lorry.dossiertau.data.model.sameContentAs
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.name
import lorry.dossiertau.support.littleClasses.parentPath as tauPathParentPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauDate
import lorry.dossiertau.support.littleClasses.toTauFileName
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.support.littleClasses.toTauPicture
import lorry.dossiertau.usecases.applicationFavorites.AppliFavos
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import lorry.dossiertau.usecases.generateHTMLs.toBitmap
import org.koin.java.KoinJavaComponent.inject

open class FolderCompo(
    open val folderRepo: IFolderRepo,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val fileDiffDAO: FileDiffDao,
    private val spy: ISpy,
) : IFolderCompo {

    companion object {
        var collectFillLaunched = false
    }

    val appliFavos: AppliFavos by inject(AppliFavos::class.java)

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    //#[[folderFlowDeclaration]]
    private val _folderFlow = MutableStateFlow<Option<TauFolder>>(None)
    override val folderFlow = _folderFlow.asStateFlow()

    override fun changeFolderFlow(folder: Option<TauFolder>) {
        println("DEBUG: changeFolderFlow: ${folder.display()}")
        _folderFlow.update { folder }
    }

    /**
     * set folderFlow à "folder"
     */
    override fun setFolderFlow(folderFullPath: TauPath) {
        //#[[coroutine longue]]
        scope.launch(dispatcher) {
            spy.setObservedFolder(folderFullPath)
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
        merge(
            fileDiffDAO.diffFlow().drop(1).filterNotNull(),
            folderPathFlow.drop(1),
            fileDiffDAO.getAllContentFlow().distinctUntilChanged().filterNotNull()
        ).transform { change ->
            when (change) {
                //folderPathFlow
                is TauPath -> {
                }

                //fileDiffDAO.diffFlow
                is TauEntity.Diff -> {
                    val diff = change as TauEntity.Diff
                    val path = folderFlow.value.getOrElse { TauFolder.EMPTY }.fullPath

                    when (change.op_type) {
                        OpType.FolderRefresh.text -> {
                            if (diff.full_path == path.path)
                                emit(diff)
                        }

                        else -> {
                            if (diff.parentPath == path.path)
                                emit(diff)
                        }
                    }
                }

                //fileDiffDAO.getAllContent()
                is List<*> -> {
                    val allContents = change as List<ContentWithItems>
                    if (allContents.isEmpty()) return@transform

                    val content = allContents
                        .sortedBy { it.content.modifiedAtIso?.toEpochMilli() }
                        .last()

                    val currentFolderPath = folderFlow.value.getOrNull()?.fullPath
                    val contentPath = content.content.full_path.toTauPath()
                    val observedPath = spy.observedFolderFlow.value

                    if (currentFolderPath != null && currentFolderPath != contentPath && contentPath != observedPath) {
                        println("COLLECTDIFFS: Ignoré car le répertoire stocké ($contentPath) ne correspond ni au répertoire actuel ($currentFolderPath) ni au répertoire observé ($observedPath)")
                        return@transform
                    }

                    val newChildren = content.items
                        .filter { !it.name.startsWith(".") }
                        .map { item ->
                            val itemPicture = item.picture?.toBitmap()?.toTauPicture() ?: TauPicture.NONE
                            when (item.type) {
                                ItemType.FILE ->
                                    TauFile.Data(
                                        //à voir
                                        id = TauIdentifier.random(),
                                        parentPath = content.content.full_path.toTauPath(),
                                        name = item.name.toTauFileName(),
                                        picture = itemPicture,
                                        modificationDate = item.modificationDate?.toEpochMilli()
                                            .toTauDate(),
//                                        memo = item.memo,
                                        fileId = item.fileId,
                                        size = 0L,
                                    )

                                ItemType.FOLDER ->
                                    TauFolder.Data(
                                        id = TauIdentifier.random(),
                                        parentPath = content.content.full_path.toTauPath(),
                                        name = item.name.toTauFileName(),
                                        picture = itemPicture,
                                        modificationDate = item.modificationDate?.toEpochMilli()
                                            .toTauDate(),
//                                        memo = item.memo,
                                        fileId = item.fileId,
                                        children = emptyList()
                                    )
                            }
                        } ?: emptyList()

                    if (folderFlow.value.getOrNull()?.children?.sameContentAs(newChildren) == true) {
                        println("COLLECTDIFFS: Ignoré car le contenu est identique.")
                        return@transform
                    }

                    val result = TauFolder.Data(
                        id = TauIdentifier.random(),
                        parentPath = content.content.full_path.toTauPath().tauPathParentPath
                            ?: TauPath.EMPTY,
                        name = content.component1().full_path.toTauPath().name,
                        picture = TauPicture.NONE,
                        modificationDate = content.component1().modifiedAtIso?.toEpochMilli()
                            .toTauDate(),
                        fileId = FileId.EMPTY,
                        children = newChildren
                    ) as TauFolder

                    println("DEBUG: setFolderFlow:${result.fullPath}")
                    val res2 = result.toOption()
                    changeFolderFlow(res2)
                    emit(null)
                }
            }
        }
            .filterNotNull()
            .collect { diff ->

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

