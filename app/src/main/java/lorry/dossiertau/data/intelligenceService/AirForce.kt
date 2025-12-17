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
        return cia.ciaDecisions.onEach { ciaLevel ->
            when (ciaLevel){
                is CIALevel.CreateItem -> {
                    val dbCommand = DbCommand.CreateItem(
                        item = DbItem(
                            fullPath = ciaLevel.itemPath,
                            modificationDate = ciaLevel.modificationDate,
                            type = ciaLevel.itemType,
                        )
                    )

                    modifyDatabaseBy(dbCommand)
                }

                is CIALevel.DeleteItem -> {
                    val dbCommand = DbCommand.DeleteItem(
                        item = DbItem(
                            fullPath = ciaLevel.itemPath,
                            modificationDate = ciaLevel.modificationDate,
                            type = ciaLevel.itemType
                        )
                    )

                    modifyDatabaseBy(dbCommand)
                }

                is CIALevel.ModifyItem -> {
                    val dbCommand = DbCommand.ModifyItem(
                        item = DbItem(
                            fullPath = ciaLevel.itemPath,
                            modificationDate = ciaLevel.modificationDate,
                            type = ciaLevel.itemType
                        )
                    )

                    modifyDatabaseBy(dbCommand)
                }

                is CIALevel.GlobalRefresh -> {
                    val dbCommand = DbCommand.GlobalRefresh(
                        path = ciaLevel.itemPath,
                        refreshDate = ciaLevel.refreshDate)
                    modifyDatabaseBy(dbCommand)
                }

                else -> {

                }
            }


        }.launchIn(scope)
    }

    fun modifyDatabaseBy(command: DbCommand) {
        when(command){
            is DbCommand.CreateItem -> {
                scope.launch {
                    repo.insertDiff(command)
                }
            }

            is DbCommand.DeleteItem -> {
                scope.launch {
                    repo.insertDiff(command)
                }
            }

            is DbCommand.ModifyItem -> {
                scope.launch {
                    repo.insertDiff(command)
                }
            }

            is DbCommand.GlobalRefresh -> {
                scope.launch {
                    repo.insertDiff(command)
                }
            }
        }
    }
}