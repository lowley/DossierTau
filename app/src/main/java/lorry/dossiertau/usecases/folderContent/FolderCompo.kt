package lorry.dossiertau.usecases.folderContent

import android.graphics.Bitmap
import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.raise.fold
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
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
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.model.children
import lorry.dossiertau.data.model.copy
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.isFolder
import lorry.dossiertau.data.model.modificationDate
import lorry.dossiertau.data.model.name
import lorry.dossiertau.data.model.parentPath
import lorry.dossiertau.data.model.picture
import lorry.dossiertau.data.model.sameContentAs
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
import lorry.dossiertau.usecases.folderContent.support.ThumbnailLoadScheduler
import lorry.dossiertau.usecases.folderContent.support.ThumbnailViewport
import lorry.dossiertau.usecases.generateHTMLs.toBitmap
import org.koin.java.KoinJavaComponent.inject
import java.io.ByteArrayOutputStream

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
    private val thumbnailScheduler = ThumbnailLoadScheduler(parallelism = 3)

    private val _ordering = MutableStateFlow(true)
    override val ordering = _ordering.asStateFlow()

    private val _foldersFirst = MutableStateFlow(false)
    override val foldersFirst = _foldersFirst.asStateFlow()

    override fun setOrdering(value: Boolean) {
        _ordering.value = value
    }

    override fun toggleFoldersFirst() {
        _foldersFirst.value = !_foldersFirst.value
    }

    private val _folderFlow = MutableStateFlow<Option<TauFolder>>(None)
    override val folderFlow = combine(
        _folderFlow,
        _ordering,
        _foldersFirst
    ) { folders, sortByDate, foldersFirst ->
        folders.map { folder ->
            val sortedItems = folder.children.sortedWith { a, b ->
                val aFolderRank = if (a.isFolder() == foldersFirst) 0 else 1
                val bFolderRank = if (b.isFolder() == foldersFirst) 0 else 1

                if (aFolderRank != bFolderRank) {
                    aFolderRank.compareTo(bFolderRank)
                } else if (sortByDate) {
                    b.modificationDate.value.compareTo(a.modificationDate.value)
                } else {
                    a.name.value.compareTo(b.name.value, ignoreCase = true)
                }
            }
            folder.copy(items = sortedItems) as TauFolder
        }
    }
        .onEach { folder ->
            val text = "DEBUG: Nouvelle émission vers l'UI, ${folder.getOrNull()?.children?.firstOrNull()?.modificationDate?.value?.toTauDate()?.toddMMyyyyHHmmss()}"
            println(text)
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null.toOption()
        )

    override fun setFolderOrdering(ordering: Boolean) {
        _ordering.value = ordering
    }

    override fun changeFolderFlow(folder: Option<TauFolder>) {
        println("DEBUG: changeFolderFlow: ${folder.display()}")
        _folderFlow.update { folder }
    }

    override fun setFolderFlow(folderFullPath: TauPath) {
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

    override fun requestThumbnails(
        items: List<TauItem>,
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
    ) {
        val viewport = ThumbnailViewport.of(
            firstVisibleIndex = firstVisibleIndex,
            lastVisibleIndex = lastVisibleIndex,
            itemCount = items.size,
        )

        thumbnailScheduler.newViewportGeneration()

        items.forEachIndexed { index, item ->
            val priority = viewport.priorityOf(index) ?: return@forEachIndexed
            if (item.picture != TauPicture.NONE) return@forEachIndexed

            thumbnailScheduler.request(
                key = item.fullPath.path,
                priority = priority,
            ) {
                val picture = folderRepo.loadThumbnail(item.fullPath) ?: return@request
                val bitmap = picture.toBitmap() ?: return@request

                val currentFolder = _folderFlow.value.getOrNull()
                if (currentFolder != null && currentFolder.fullPath == item.parentPath) {
                    val children = currentFolder.children.map { child ->
                        if (child.fullPath == item.fullPath) child.copy(picture = picture) else child
                    }
                    changeFolderFlow((currentFolder.copy(items = children) as TauFolder).toOption())
                }

                val bytes = ByteArrayOutputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    output.toByteArray()
                }
                fileDiffDAO.updateLatestContentItemPicture(
                    folderPath = item.parentPath.path,
                    itemName = item.name.value,
                    picture = bytes,
                )
            }
        }
    }

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
                is TauPath -> Unit

                is TauEntity.Diff -> {
                    val path = folderFlow.value.getOrElse { TauFolder.EMPTY }.fullPath
                    when (change.op_type) {
                        OpType.FolderRefresh.text -> if (change.full_path == path.path) emit(change)
                        else -> if (change.parentPath == path.path) emit(change)
                    }
                }

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
                                ItemType.FILE -> TauFile.Data(
                                    id = TauIdentifier.random(),
                                    parentPath = content.content.full_path.toTauPath(),
                                    name = item.name.toTauFileName(),
                                    picture = itemPicture,
                                    modificationDate = item.modificationDate?.toEpochMilli().toTauDate(),
                                    fileId = item.fileId,
                                    size = 0L,
                                )

                                ItemType.FOLDER -> TauFolder.Data(
                                    id = TauIdentifier.random(),
                                    parentPath = content.content.full_path.toTauPath(),
                                    name = item.name.toTauFileName(),
                                    picture = itemPicture,
                                    modificationDate = item.modificationDate?.toEpochMilli().toTauDate(),
                                    fileId = item.fileId,
                                    children = emptyList()
                                )
                            }
                        }

                    if (folderFlow.value.getOrNull()?.children?.sameContentAs(newChildren) == true) {
                        println("COLLECTDIFFS: Ignoré car le contenu est identique.")
                        return@transform
                    }

                    val result = TauFolder.Data(
                        id = TauIdentifier.random(),
                        parentPath = content.content.full_path.toTauPath().tauPathParentPath ?: TauPath.EMPTY,
                        name = content.content.full_path.toTauPath().name,
                        picture = TauPicture.NONE,
                        modificationDate = content.content.modifiedAtIso?.toEpochMilli().toTauDate(),
                        fileId = FileId.EMPTY,
                        children = newChildren
                    ) as TauFolder

                    println("DEBUG: setFolderFlow:${result.fullPath}")
                    changeFolderFlow(result.toOption())
                    emit(null)
                }
            }
        }
            .filterNotNull()
            .collect { diff ->
                println("COLLECTDIFFS: reçu diff: $diff")
                val folder = folderFlow.value.getOrNull() ?: return@collect
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

    if (this.getOrNull() == null) return NONE
    this.getOrNull()?.parentPath?.toString() ?: return PB
    return this.getOrNull()?.toString() ?: NONE
}
