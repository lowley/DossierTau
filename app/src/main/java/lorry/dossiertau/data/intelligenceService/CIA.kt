package lorry.dossiertau.data.intelligenceService

import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
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

class CIA : LifecycleService() {

    val spy: ISpy = Spy()
    val airForce = AirForce()

    //////////////////////////////////////////////////////////////////////////////////////
    // la production du Fbi: informer TauFolder des changements dans le disque via Room //
    //////////////////////////////////////////////////////////////////////////////////////
    val _ciaDecisions = MutableSharedFlow<TransferingDecision>()
    val ciaDecisions: SharedFlow<TransferingDecision> = _ciaDecisions.asSharedFlow()

    fun emitCIADecision(decision: TransferingDecision){
        lifecycleScope.launch(Dispatchers.Default) {
            _ciaDecisions.emit(decision)
        }
    }

    override fun onCreate() {
        super.onCreate()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        //makeYourMind n'est pas branchée directement sur le flux pour la testabilité
        spy.updateEventFlow.onEach { event ->
            sortUpdateEvents(event)?.let { decision ->
                emitCIADecision(decision)
            }
        }.launchIn(scope = lifecycleScope)

        return START_STICKY
    }

    companion object {
        fun sortUpdateEvents(event: IUpdateEvent): TransferingDecision? {

            val result = when (event){
                is AtomicUpdateEvent -> manageAtomicEvent(event)
                is GlobalUpdateEvent -> null
                else -> null
            }

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
                    TransferingDecision.CREATEFILE(
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