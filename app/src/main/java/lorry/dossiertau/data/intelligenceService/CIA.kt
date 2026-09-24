package lorry.dossiertau.data.intelligenceService

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lorry.dossiertau.R
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicEventType
import lorry.dossiertau.data.intelligenceService.utils.CIALevel
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicSpyLevel
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalSpyLevel
import lorry.dossiertau.data.intelligenceService.utils.events.ISpyLevel
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauDate
import org.koin.core.context.GlobalContext
import java.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class CIA() : LifecycleService() {

    val koin = GlobalContext.get()
    var scope: CoroutineScope = koin.get()
    var dispatcher: CoroutineDispatcher = Dispatchers.Default
    var spy: ISpy = koin.get()
    val airForce: AirForce = koin.get()
    private var eventsJob: Job? = null

    private var lastActivityTime = System.currentTimeMillis()
    private val inactivityTimeout = 30.seconds
    private var inactivityJob: Job? = null

    /////////////////////////////////////////////////////////////////////////////////////////
    // la production de la Cia: informer TauFolder des changements dans le disque via Room //
    /////////////////////////////////////////////////////////////////////////////////////////
    val _ciaDecisions = MutableSharedFlow<List<CIALevel>>()
    val ciaDecisions: SharedFlow<List<CIALevel>> = _ciaDecisions.asSharedFlow()

    // Même protection qu'entre Spy et CIA : une décision CIA ne doit jamais
    // disparaître simplement parce qu'AirForce n'a pas encore commencé à écouter.
    private val ciaDecisionChannel = Channel<List<CIALevel>>(Channel.UNLIMITED)

    fun emitCIALevel(decision: CIALevel) {
        updateActivity()
        ciaDecisionChannel.trySend(listOf(decision))
    }

    fun emitCIALevels(decisions: List<CIALevel>) {
        if (decisions.isEmpty()) return
        updateActivity()
        ciaDecisionChannel.trySend(decisions)
    }

    private fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    private fun startInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = lifecycleScope.launch {
            while (true) {
                delay(5000)
                if (System.currentTimeMillis() - lastActivityTime > inactivityTimeout.inWholeMilliseconds) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    // On ne stopSelf() pas forcément si on veut garder le service "vivant" en arrière plan simple
                    // mais pour dataSync, si on n'a plus rien à faire, autant s'arrêter.
                    // Cependant, LifecycleService pourrait être redémarré par d'autres composants.
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        scope.launch(dispatcher) {
            spy.observedFolderFlow.collect { folder ->
                updateActivity()
                startForegroundServiceWithNotification(folder = folder)
            }
        }

        airForce.cia = this
        airForce.startListeningForCIADecisions()

        // Convoyeur fiable CIA -> AirForce. Il attend qu'AirForce soit abonné
        // avant de publier la première décision (notamment le GlobalRefresh initial).
        lifecycleScope.launch(dispatcher) {
            for (decisions in ciaDecisionChannel) {
                _ciaDecisions.subscriptionCount.first { subscriberCount ->
                    subscriberCount > 0
                }
                _ciaDecisions.emit(decisions)
            }
        }

        if (eventsJob?.isActive != true) {
            eventsJob = spy.spyLevelFlow
                .onEach { event ->
                    updateActivity()
                    manageUpdateEvents(event).let { emitCIALevels(it) }
                }
                .launchIn(lifecycleScope) // LifecycleService fournit lifecycleScope
        }

        startInactivityTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    fun manageUpdateEvents(events: List<ISpyLevel>): List<CIALevel> {

        val results = events.mapNotNull { event ->
            when (event) {
                is AtomicSpyLevel -> manageAtomicEvent(event)
                is GlobalSpyLevel -> manageGlobalEvent(event)
                else -> throw IllegalArgumentException("CIA receiver an event with unknown type")
            }
        }.also { return it }
    }

    @OptIn(ExperimentalTime::class)
    private fun manageGlobalEvent(event: GlobalSpyLevel): CIALevel? {
        val result = CIALevel.GlobalRefresh(
            eventPath = event.path,
            refreshDate = Clock.systemDefaultZone().millis().toTauDate(),
            items = event.items,
        )

        return result
    }

    private fun manageAtomicEvent(event: AtomicSpyLevel): CIALevel? {
        return when (val type = event.eventType) {
            AtomicEventType.CREATE -> {
                type.reactWhenReceived(
                    event.path,
                    spy.observedFolderFlow.value,
                    CIALevel.CreateItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType,
                        itemId = event.itemId,
                        picture = event.picture,
                        memo = event.memo
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
                        itemId = event.itemId,
                        picture = event.picture,
                        memo = event.memo
                    )
                )
            }

            AtomicEventType.MODIFY -> {
                type.reactWhenReceived(
                    event.path,
                    spy.observedFolderFlow.value,
                    CIALevel.ModifyItem(
                        eventPath = event.path,
                        modificationDate = event.modificationDate,
                        itemType = event.itemType,
                        itemId = event.itemId,
                        picture = event.picture,
                        memo = event.memo
                    )
                )
            }
        }
    }

    private fun startForegroundServiceWithNotification(folder: TauPath) {
        val channelId = "cia_channel"
        val channelName = "CIA Surveillance"
        val notificationId = 1

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("CIA active")
            .setContentText("Service ok. Répertoire courant: ${folder.path}")
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