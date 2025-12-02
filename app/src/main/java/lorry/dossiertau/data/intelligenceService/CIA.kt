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
import lorry.dossiertau.data.intelligenceService.utils.EventType
import lorry.dossiertau.data.intelligenceService.utils.IncomingEvent
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision

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

        spy.incomingEventFlow.onEach { event ->
            makeYourMind(event)?.let { decision ->
                emitCIADecision(decision)
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