package lorry.dossiertau.data.intelligenceService

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lorry.dossiertau.R
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicEventType
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.IUpdateEvent
import lorry.dossiertau.support.littleClasses.path
import org.koin.core.context.GlobalContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.io.println

class CIA() : LifecycleService() {

    val koin = GlobalContext.get()
    var scope: CoroutineScope = koin.get()
    var dispatcher: CoroutineDispatcher = Dispatchers.Default
    var spy: ISpy = koin.get()
    val airForce: AirForce = koin.get()
    private var eventsJob: Job? = null

    /////////////////////////////////////////////////////////////////////////////////////////
    // la production de la Cia: informer TauFolder des changements dans le disque via Room //
    /////////////////////////////////////////////////////////////////////////////////////////
    val _ciaDecisions = MutableSharedFlow<TransferingDecision>()
    val ciaDecisions: SharedFlow<TransferingDecision> = _ciaDecisions.asSharedFlow()

    fun emitCIADecision(decision: TransferingDecision) {
        scope.launch(dispatcher) {
            println("EMIT  | name=${GlobalScope.coroutineContext[CoroutineName]} thread=${Thread.currentThread().name} disp=${GlobalScope.coroutineContext[ContinuationInterceptor]}")
            _ciaDecisions.emit(decision)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        airForce.cia = this
        airForce.startListeningForCIADecisions()

        if (eventsJob?.isActive != true) {
            eventsJob = spy.updateEventFlow
                .onEach { event ->
                    manageUpdateEvents(event)?.let { emitCIADecision(it) }
                }
                .launchIn(lifecycleScope) // LifecycleService fournit lifecycleScope
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

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
        return when (val type = event.eventType) {
            AtomicEventType.ATTRIB -> {
                return null
            }

            AtomicEventType.CLOSE_WRITE -> {
                return null
            }

            AtomicEventType.CREATE -> {
                type.reactWhenReceived(
                    event.path,
                    spy.observedFolderFlow.value,
                    TransferingDecision.CreateItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType
                    )
                )
            }

            AtomicEventType.DELETE -> {
                type.reactWhenReceived(
                    event.path,
                    spy.observedFolderFlow.value,
                    TransferingDecision.DeleteItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType
                    )
                )
            }

            AtomicEventType.DELETE_SELF -> {
                return null
            }

            AtomicEventType.MODIFY -> {
                type.reactWhenReceived(
                    event.path,
                    spy.observedFolderFlow.value,
                    TransferingDecision.ModifyItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType
                    )
                )
            }

            AtomicEventType.MOVED_FROM -> {
                type.reactWhenReceived(
                    event.path,
                    spy.observedFolderFlow.value,
                    TransferingDecision.DeleteItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType
                    )
                )
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

    private fun startForegroundServiceWithNotification() {
        val channelId = "cia_channel"
        val channelName = "CIA Surveillance"
        val notificationId = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("CIA active")
            .setContentText("Service ok. Répertoire courant: ${spy.observedFolderFlow.value.path}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    override fun onDestroy() {
        eventsJob?.cancel()
        super.onDestroy()
    }
}