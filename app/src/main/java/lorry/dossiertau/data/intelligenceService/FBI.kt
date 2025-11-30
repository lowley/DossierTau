package lorry.dossiertau.data.intelligenceService

import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import lorry.dossiertau.data.intelligenceService.utils.EventType
import lorry.dossiertau.data.intelligenceService.utils.IncomingEvent
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision
import lorry.dossiertau.data.intelligenceService.utils.InternalEvent

class FBI : LifecycleService() {

    val spy: ISpy = Spy()
    val airForce = AirForce()

    //////////////////////////////////////////////////////////////////////////////////////
    // la production du Fbi: informer TauFolder des changements dans le disque via Room //
    //////////////////////////////////////////////////////////////////////////////////////
    val _fbiDecisions = MutableSharedFlow<TransferingDecision>()
    val fbiDecisions: SharedFlow<TransferingDecision> = _fbiDecisions.asSharedFlow()

    fun emitFbiDecision(decision: TransferingDecision){
        lifecycleScope.launch(Dispatchers.Default) {
            _fbiDecisions.emit(decision)
        }
    }

    override fun onCreate() {
        super.onCreate()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        spy.incomingEventFlow.onEach { event ->
            makeYourMind(event)?.let { decision ->
                emitFbiDecision(decision)
            }
        }.launchIn(scope = lifecycleScope)

        return START_STICKY
    }

    companion object {
        fun makeYourMind(event: IncomingEvent): TransferingDecision? {

            return when (event.eventType){
                EventType.ATTRIB -> {
                    return null
                }
                EventType.CLOSE_WRITE -> {
                    return null
                }
                EventType.CREATE -> {
                    TransferingDecision.CREATEFILE(
                        eventFilePath = event.path,
                        modificationDate = event.modificationDate
                    )
                }
                EventType.DELETE -> {
                    return null
                }
                EventType.DELETE_SELF -> {
                    return null
                }
                EventType.MODIFY -> {
                    return null
                }
                EventType.MOVED_FROM -> {
                    return null
                }
                EventType.MOVED_TO -> {
                    return null
                }
                EventType.MOVE_SELF -> {
                    return null

                }

                EventType.UNKNOWN -> {
                    return null
                }
            }
        }
    }

}