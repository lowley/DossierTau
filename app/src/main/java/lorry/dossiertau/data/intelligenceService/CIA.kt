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
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lorry.dossiertau.R
import lorry.dossiertau.TauApp
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicEventType
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.IUpdateEvent
import lorry.dossiertau.support.littleClasses.path
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.io.println

class CIA() : LifecycleService() {

    val koin = GlobalContext.get()
    var scope: CoroutineScope = koin.get()
    var dispatcher: CoroutineDispatcher = Dispatchers.Default

    val spy: ISpy = koin.get()
    val airForce: AirForce = koin.get()
    private var eventsJob: Job? = null

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