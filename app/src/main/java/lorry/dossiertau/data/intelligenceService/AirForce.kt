package lorry.dossiertau.data.intelligenceService

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.data.planes.DbItem
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext
import kotlin.io.println

class AirForce(
    val cia: CIA,
    val scope: CoroutineScope
) {

    fun start(decisions: SharedFlow<TransferingDecision>): Job {
        return decisions.onEach { decision ->
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


    fun modifyDatabaseBy(dbCommand: DbCommand) {





    }
}