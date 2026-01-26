package lorry.dossiertau.usecases.folderContent

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.data.dbModel.OpType
import lorry.dossiertau.data.dbModel.TauEntity
import lorry.dossiertau.data.dbModel.toTauItem
import lorry.dossiertau.data.model.computeParentFolderDate
import lorry.dossiertau.data.diskTransfer.toTauItems
import lorry.dossiertau.data.intelligenceService.ISpy
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.asDataCommon
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.isFile
import lorry.dossiertau.data.model.isFolder
import lorry.dossiertau.data.model.name
import lorry.dossiertau.data.model.parentPath
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.name
import lorry.dossiertau.support.littleClasses.parentPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.ui.support.capsule.CapsuleComponent
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo

open class FolderCompo(
    open val folderRepo: IFolderRepo,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val fileDiffDAO: FileDiffDao,
    private val spy: ISpy
) : IFolderCompo {

    companion object {
        var collectFillLaunched = false
    }

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
            val repoItems = folderRepo.getItemsInFullPath(folderFullPath)
            val compoItems = repoItems.toTauItems()
                .filter { !it.name.value.startsWith('.') }
                .sortedBy { it.isFile().toString() + it.name.value }

            //TODO tester si children contient déjà item
            val folderDate = compoItems.computeParentFolderDate()

            val compoItemsWithPictures = compoItems.map { item ->
                async {

                    val path = item.fullPath
                    val i = item.asDataCommon ?: return@async TauFolder.EMPTY

                    val image = if (path.path.endsWith(".html")) {
                        val bitmap = folderRepo.extractImageFromHtml(path)
                        bitmap

                    } else if (
                        path.path.endsWith(".mp4") ||
                        path.path.endsWith(".mpg") ||
                        path.path.endsWith(".mkv") ||
                        path.path.endsWith(".ts") ||
                        path.path.endsWith(".avi") ||
                        item.isFolder()
                    ) {
                        val newCapsuleMgr = CapsuleComponent()
                        val newCapsule = newCapsuleMgr.getCapsule(path)

                        val newCropped = newCapsule?.getCroppedPicture()
                        val newInitial = newCapsule?.getInitialPicture()
                        var image = newCropped ?: newInitial
                        image
                    }
                    else null

                    val result = if (item.isFile()) {
                        //file
//                        image = image ?: R.drawable.fichier

                        TauFile.Data(
                            id = i.id,
                            parentPath = i.parentPath,
                            name = i.name,
                            picture = image?.let { TauPicture.fromBitmap(it) } ?: TauPicture.NONE,
                            modificationDate = i.modificationDate,
                            size = 0L,
                            fileId = i.fileId
                        ) as TauFile
                    } else {
                        //folder
                        TauFolder.Data(
                            id = i.id,
                            parentPath = i.parentPath,
                            name = i.name,
                            picture = image?.let { TauPicture.fromBitmap(it) } ?: TauPicture.NONE,
                            modificationDate = i.modificationDate,
                            fileId = i.fileId,
                            children = emptyList()
                        ) as TauFolder
                    }

                    result
                }
            }.awaitAll()

            val result = TauFolder.Data(
                id = TauIdentifier.random(),
                parentPath = folderFullPath.parentPath,
                name = folderFullPath.name,
                picture = TauPicture.NONE,
                modificationDate = folderDate,
                fileId = FileId.EMPTY,
                children = compoItemsWithPictures
            ) as TauFolder

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
        merge(
            fileDiffDAO.diffFlow().drop(1).filterNotNull(),
            folderPathFlow
        ).transform { diffOrPath ->
            println("COLLECTDIFFS: reçu path: $diffOrPath")
            when (diffOrPath) {
                is TauPath -> {
//                    setFolderFlow(diffOrPath)
                    emit(null)
                }

                is TauEntity.Diff -> {
                    val diff = diffOrPath as TauEntity.Diff
                    val path = folderFlow.value.getOrElse { TauFolder.EMPTY }.fullPath

                    when (diffOrPath.op_type) {
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

