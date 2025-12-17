package lorry.dossiertau.data.intelligenceService

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lorry.dossiertau.data.dbModel.DiffRepository
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.data.planes.DbItem
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext
import kotlin.io.println

/**
 * Necesita injectar con cia
 */
class AirForce(
    private val repo: DiffRepository,
    val scope: CoroutineScope
) {
    lateinit var cia: CIA

    fun startListeningForCIADecisions(): Job {
        return cia.ciaDecisions.onEach { decision ->
            when (decision){
                is TransferingDecision.CreateItem -> {
                    val dbCommand = DbCommand.CreateItem(
                        item = DbItem(
                            fullPath = decision.itemPath,
                            modificationDate = decision.modificationDate,
                            type = decision.itemType,
                        )
                    )

                    modifyDatabaseBy(dbCommand)
                }

                is TransferingDecision.DeleteItem -> {
                    val dbCommand = DbCommand.DeleteItem(
                        item = DbItem(
                            fullPath = decision.itemPath,
                            modificationDate = decision.modificationDate,
                            type = decision.itemType
                        )
                    )

                    modifyDatabaseBy(dbCommand)
                }

                is TransferingDecision.ModifyItem -> {
                    val dbCommand = DbCommand.ModifyItem(
                        item = DbItem(
                            fullPath = decision.itemPath,
                            modificationDate = decision.modificationDate,
                            type = decision.itemType
                        )
                    )

                    modifyDatabaseBy(dbCommand)
                }

                is TransferingDecision.GlobalRefresh -> {
                    val dbCommand = DbCommand.GlobalRefresh(
                        path = decision.itemPath,
                        refreshDate = decision.refreshDate)
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