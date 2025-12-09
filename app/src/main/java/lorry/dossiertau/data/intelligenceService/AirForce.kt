package lorry.dossiertau.data.intelligenceService

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lorry.dossiertau.data.dbModel.DiffRepository
import lorry.dossiertau.data.dbModel.toFileDiffEntity
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.data.planes.DbItem
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext
import kotlin.io.println

/**
 * Necesitos injectar con cia
 */
class AirForce(
    private val repo: DiffRepository,
    val scope: CoroutineScope
) {
    lateinit var cia: CIA

    fun startListeningForCIADecisions(): Job {
        return cia.ciaDecisions.onEach { decision ->
            println("COLLECT| name=${coroutineContext[CoroutineName]} thread=${Thread.currentThread().name} disp=${coroutineContext[ContinuationInterceptor]}")

            when (decision){
                is TransferingDecision.CreateFile -> {

                    val dbCommand = DbCommand.CreateItem(
                        item = DbItem(
                            fullPath = decision.filePath,
                            modificationDate = decision.modificationDate,
                            type = ItemType.FILE
                        )
                    )

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
        }
    }
}