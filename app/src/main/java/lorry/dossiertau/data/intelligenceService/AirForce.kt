package lorry.dossiertau.data.intelligenceService

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.dbModel.DiffRepository
import lorry.dossiertau.data.dbModel.TauEntity
import lorry.dossiertau.data.intelligenceService.utils.CIALevel
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.data.planes.DbItem
import lorry.dossiertau.support.littleClasses.name
import lorry.dossiertau.support.littleClasses.parentPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.usecases.generateHTMLs.toByteArray
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi

/**
 * Necesita injectar con cia
 */
class AirForce(
    private val repo: DiffRepository,
    val scope: CoroutineScope
) {
    lateinit var cia: CIA
    fun startListeningForCIADecisions(): Job {
        return cia.ciaDecisions.onEach { ciaLevels ->

            val commands = ciaLevels.mapNotNull { ciaLevel ->
                when (ciaLevel) {
                    is CIALevel.CreateItem -> {
                        DbCommand.CreateItem(
                            item = DbItem(
                                fullPath = ciaLevel.itemPath,
                                modificationDate = ciaLevel.modificationDate,
                                type = ciaLevel.itemType,
                                fileId = ciaLevel.itemId,
                                pictureData = ciaLevel.picture,
                                memo = ciaLevel.memo,
                            )
                        )
                    }

                    is CIALevel.DeleteItem -> {
                        DbCommand.DeleteItem(
                            item = DbItem(
                                fullPath = ciaLevel.itemPath,
                                modificationDate = ciaLevel.modificationDate,
                                type = ciaLevel.itemType,
                                fileId = ciaLevel.itemId,
                                pictureData = ciaLevel.picture,
                                memo = ciaLevel.memo,
                            )
                        )
                    }

                    is CIALevel.ModifyItem -> {
                        DbCommand.ModifyItem(
                            item = DbItem(
                                fullPath = ciaLevel.itemPath,
                                modificationDate = ciaLevel.modificationDate,
                                type = ciaLevel.itemType,
                                fileId = ciaLevel.itemId,
                                pictureData = ciaLevel.picture,
                                memo = ciaLevel.memo,
                            )
                        )
                    }

                    is CIALevel.GlobalRefresh -> {

                        val dbItems = ciaLevel.items.map { globalItem ->
                            DbItem(
                                fullPath = globalItem.path,
                                modificationDate = globalItem.modificationDate,
                                type = globalItem.itemType,
                                fileId = globalItem.itemId,
                                pictureData = globalItem.picture,
                                memo = globalItem.memo
                            )
                        }

                        DbCommand.GlobalRefresh(
                            path = ciaLevel.itemPath,
                            refreshDate = ciaLevel.refreshDate,
                            parentPath = ciaLevel.itemPath.parentPath,
                            items = dbItems,
                        )
                    }
                }
            }

            modifyDatabaseByAll(commands)
        }.launchIn(scope)
    }

    fun modifyDatabaseByAll(commands: List<DbCommand>) {
        scope.launch(Dispatchers.IO) {
            commands.partition { it is DbCommand.CreateItem || it is DbCommand.DeleteItem || it is DbCommand.ModifyItem }.let {
                val (diffs, contents) = it
                repo.insertDiffs(diffs)
                repo.insertContents(contents.toContents())
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
suspend fun List<DbCommand>.toContents(): List<TauEntity.Content> = withContext(Dispatchers.Default) {
    assert(this@toContents.all { it is DbCommand.GlobalRefresh })

    this@toContents
        .map { it as DbCommand.GlobalRefresh }
        .map { globesh ->
            async {
                TauEntity.Content(
                    correlationId = "",
                    full_path = globesh.path.path,
                    modifiedAtIso = Instant.ofEpochMilli(globesh.refreshDate.value),
                    items = globesh.items
                        .map { conti ->
                            async {
                                TauEntity.ContentItem(
                                    //traitement particulier lors de l'enregistrement: 2 phases
                                    parentContentId = 0L,
                                    id = conti.id.value.toString(),
                                    name = conti.fullPath.name.value,
                                    picture = conti.pictureData?.toBitmap()?.toByteArray(),
                                    memo = conti.memo,
                                    modificationDate = Instant.ofEpochMilli(conti.modificationDate.value),
                                    fileId = conti.fileId,
                                    type = conti.type
                                )
                            }
                        }.awaitAll()
                )
            }
        }.awaitAll()
}