package lorry.dossiertau.data.intelligenceService

import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicEventType
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.IUpdateEvent
import kotlin.coroutines.ContinuationInterceptor
import kotlin.io.println

class CIA(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : LifecycleService() {

    val spy: ISpy = Spy()

    //////////////////////////////////////////////////////////////////////////////////////////
    // la production de la  Cia: informer TauFolder des changements dans le disque via Room //
    //////////////////////////////////////////////////////////////////////////////////////////
    val _ciaDecisions = MutableSharedFlow<TransferingDecision>()
    val ciaDecisions: SharedFlow<TransferingDecision> = _ciaDecisions.asSharedFlow()

    fun emitCIADecision(decision: TransferingDecision){
        scope.launch(dispatcher) {
            println("EMIT  | name=${GlobalScope.coroutineContext[CoroutineName]} thread=${Thread.currentThread().name} disp=${GlobalScope.coroutineContext[ContinuationInterceptor]}")
            _ciaDecisions.emit(decision)
        }
    }

    private suspend fun emitAction(decision: TransferingDecision) {

    }

    override fun onCreate() {
        super.onCreate()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        //makeYourMind n'est pas branchée directement sur le flux pour la testabilité
        spy.updateEventFlow.onEach { event ->
            manageUpdateEvents(event)?.let { decision ->
                emitCIADecision(decision)
            }
        }.launchIn(scope = scope)

        return START_STICKY
    }

    companion object {
        fun manageUpdateEvents(event: IUpdateEvent): TransferingDecision? {

            val result = when (event) {
                is AtomicUpdateEvent -> manageAtomicEvent(event)
                is GlobalUpdateEvent -> manageGlobalEvent(event)
                else -> null
            }

            return result
        }

        private fun manageGlobalEvent(event: GlobalUpdateEvent): TransferingDecision? {
            val result = TransferingDecision.GlobalRefresh(event.path)
            return result
        }

        private fun manageAtomicEvent(event: AtomicUpdateEvent): TransferingDecision? {
            return when (event.eventType){
                AtomicEventType.ATTRIB -> {
                    return null
                }
                AtomicEventType.CLOSE_WRITE -> {
                    return null
                }
                AtomicEventType.CREATE -> {
                    TransferingDecision.CreateFile(
                        eventFilePath = event.path,
                        modificationDate = event.modificationDate
                    )
                }
                AtomicEventType.DELETE -> {
                    return null
                }
                AtomicEventType.DELETE_SELF -> {
                    return null
                }
                AtomicEventType.MODIFY -> {
                    return null
                }
                AtomicEventType.MOVED_FROM -> {
                    return null
                }
                AtomicEventType.MOVED_TO -> {
                    return null
                }
                AtomicEventType.MOVE_SELF -> {
                    return null

                }

                AtomicEventType.UNKNOWN -> {
                    return null
                }
            }
        }
    }

}