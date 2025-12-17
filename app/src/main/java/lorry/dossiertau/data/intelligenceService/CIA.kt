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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lorry.dossiertau.R
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicEventType
import lorry.dossiertau.data.intelligenceService.utils.CIALevel
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicSpyLevel
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalSpyLevel
import lorry.dossiertau.data.intelligenceService.utils.events.ISpyLevel
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauDate
import org.koin.core.context.GlobalContext
import java.time.Clock
import kotlin.time.ExperimentalTime

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
    val _ciaDecisions = MutableSharedFlow<CIALevel>()
    val ciaDecisions: SharedFlow<CIALevel> = _ciaDecisions.asSharedFlow()

    fun emitCIALevel(decision: CIALevel) {
        scope.launch(dispatcher) {
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
                    manageUpdateEvents(event)?.let { emitCIALevel(it) }
                }
                .launchIn(lifecycleScope) // LifecycleService fournit lifecycleScope
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    fun manageUpdateEvents(event: ISpyLevel): CIALevel? {

        val result = when (event) {
            is AtomicSpyLevel -> manageAtomicEvent(event)
            is GlobalSpyLevel -> manageGlobalEvent(event)
            else -> null
        }

        return result
    }

    @OptIn(ExperimentalTime::class)
    private fun manageGlobalEvent(event: GlobalSpyLevel): CIALevel? {
        val result = CIALevel.GlobalRefresh(
            eventPath = event.path,
            refreshDate = Clock.systemDefaultZone().millis().toTauDate())
        return result
    }

    private fun manageAtomicEvent(event: AtomicSpyLevel): CIALevel? {
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
                    CIALevel.CreateItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType,
                    )
                )
            }

            AtomicEventType.DELETE -> {
                type.reactWhenReceived(
                    event.path,
                    spy.observedFolderFlow.value,
                    CIALevel.DeleteItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType,
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
                    CIALevel.ModifyItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType,
                    )
                )
            }

            AtomicEventType.MOVED_FROM -> {
                type.reactWhenReceived(
                    event.path,
                    spy.observedFolderFlow.value,
                    CIALevel.DeleteItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType,
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