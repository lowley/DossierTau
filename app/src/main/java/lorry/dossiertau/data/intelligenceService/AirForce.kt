package lorry.dossiertau.data.intelligenceService

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lorry.dossiertau.data.dbModel.DiffRepository
import lorry.dossiertau.data.intelligenceService.utils.CIALevel
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.data.planes.DbItem

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
                when (ciaLevel){
                    is CIALevel.CreateItem -> {
                        DbCommand.CreateItem(
                            item = DbItem(
                                fullPath = ciaLevel.itemPath,
                                modificationDate = ciaLevel.modificationDate,
                                type = ciaLevel.itemType,
                            )
                        )
                    }

                    is CIALevel.DeleteItem -> {
                        DbCommand.DeleteItem(
                            item = DbItem(
                                fullPath = ciaLevel.itemPath,
                                modificationDate = ciaLevel.modificationDate,
                                type = ciaLevel.itemType
                            )
                        )
                    }

                    is CIALevel.ModifyItem -> {
                        DbCommand.ModifyItem(
                            item = DbItem(
                                fullPath = ciaLevel.itemPath,
                                modificationDate = ciaLevel.modificationDate,
                                type = ciaLevel.itemType
                            )
                        )
                    }

                    is CIALevel.GlobalRefresh -> {
                        DbCommand.GlobalRefresh(
                            path = ciaLevel.itemPath,
                            refreshDate = ciaLevel.refreshDate)
                    }

                    else -> { null }
                }
            }

            modifyDatabaseByAll(commands)
        }.launchIn(scope)
    }

    fun modifyDatabaseByAll(commands: List<DbCommand>) {
        scope.launch {
            repo.insertDiffs(commands)
        }
    }
}