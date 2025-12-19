package lorry.dossiertau.data.intelligenceService

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserver
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicEventType
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicSpyLevel
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalSpyLevel
import lorry.dossiertau.data.intelligenceService.utils.events.ISpyLevel
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath

import lorry.dossiertau.data.intelligenceService.utils.TauFileObserverInside.INACTIVE
import lorry.dossiertau.data.intelligenceService.utils.events.toEventType
import lorry.dossiertau.data.intelligenceService.utils2.events.DebouncedTimer
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.support.littleClasses.toTauDate
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import java.time.Clock
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class Spy(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    var fileObserver: TauFileObserver = TauFileObserver.of(INACTIVE),
    val fileRepo: IFolderRepo
) : ISpy {

    private val scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    ////////////////////////////////////
    // interrupteur de fonctionnement //
    ////////////////////////////////////
    val _enabledFlow = MutableStateFlow(false)
    override val enabledFlow = _enabledFlow.asStateFlow()

    override fun startSurveillance() {
        _enabledFlow.update { true }
    }

    override fun stopSurveillance() {
        _enabledFlow.update { false }
    }

    override fun setSurveillance(enabled: Boolean) {
        _enabledFlow.update { enabled }
    }

    ////////////////////////////////////////
    // répertoire observé -> surveillance //
    ////////////////////////////////////////
    val _observedFolderPathFlow = MutableStateFlow(TauPath.EMPTY)
    override val observedFolderFlow = _observedFolderPathFlow.asStateFlow()

    override fun setObservedFolder(folderPath: TauPath) {
        _observedFolderPathFlow.update { folderPath }
    }

    ////////////////////////////////
    // réglages vivacité réaction //
    ////////////////////////////////
    override val quietWindowMs: Long = 500
    override val maxWaitMs: Long = 2500

    override val minTimer = DebouncedTimer(scope)

    /////////////////////////////////
    // gestion des events entrants //
    /////////////////////////////////
    private var lastSnapshot: Snapshot = Snapshot.EMPTY(observedFolderFlow.value)

    override fun getLastSnapshot() = lastSnapshot

    /////////////////////////////
    // arriv&ée d'un évènement //
    /////////////////////////////
    private val ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    @OptIn(ExperimentalAtomicApi::class)
    private val dirty = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    override fun tick() {
        dirty.store(true)
        ticks.tryEmit(Unit)
    }

    ///////////////////////////////////////////////////////////////////////
    // évènements créés par l'espion suite à une opération sur le disque //
    ///////////////////////////////////////////////////////////////////////
    val _updateEventFlow = MutableSharedFlow<ISpyLevel>()

    override val updateEventFlow: SharedFlow<ISpyLevel> = _updateEventFlow.asSharedFlow()

    override fun emitSpyLevel(event: ISpyLevel) {
        scope.launch(dispatcher) {
            _updateEventFlow.emit(event)
        }
    }

    override fun emitFake_CREATEITEM(
        itemToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate
    ) {
        val fakeEvent = AtomicSpyLevel(
            eventType = AtomicEventType.CREATE,
            path = itemToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
        )

        emitSpyLevel(fakeEvent)
    }

    override fun emitFake_DELETEITEM(
        itemToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate
    ) {
        val fakeEvent = AtomicSpyLevel(
            eventType = AtomicEventType.DELETE,
            path = itemToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
        )

        emitSpyLevel(fakeEvent)
    }

    override fun emitFake_MODIFYITEM(
        itemToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate
    ) {
        val fakeEvent = AtomicSpyLevel(
            eventType = AtomicEventType.MODIFY,
            path = itemToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
        )

        emitSpyLevel(fakeEvent)
    }

    override fun emitFake_MOVEDFROM(
        itemToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate
    ) {
        val fakeEvent = AtomicSpyLevel(
            eventType = AtomicEventType.MOVED_FROM,
            path = itemToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
        )

        emitSpyLevel(fakeEvent)
    }

    suspend fun doOnEvent(atomicUpdateEvent: AtomicSpyLevel) {
        emitSpyLevel(atomicUpdateEvent)
    }

    init {
        //////////////
        // réglages //
        //////////////

        observedFolderFlow.onEach { folderPath ->
            lastSnapshot = fileRepo.createSnapshotFor(folderPath)

            val afterWaitingCalm = suspend {
                val newSnapshot = fileRepo.createSnapshotFor(folderPath)
                println("newSnapshot lancé: $newSnapshot")
            }

            minTimer.start(quietWindowMs) {
                minTimer.cancel()
                scope.launch(dispatcher) {
                    afterWaitingCalm()
                }
            }

            scope.launch(dispatcher) {
                ticks.collect {
                    minTimer.cancel()
                    minTimer.start(quietWindowMs) {
                        scope.launch(dispatcher) {
                            afterWaitingCalm()
                        }
                        minTimer.cancel()
                    }
                }
            }

            if (folderPath.value.isRight()) {
                fileObserver.changeTarget(
                    path = folderPath,
                    doOnEvent = { event, path ->
                        val fileDate1 = path?.toFile()?.map { it.lastModified() }
                            ?.getOrNull().let {
                                if (it == null || it == 0L)
                                    Clock.systemDefaultZone().millis()
                                else it
                            }
                        val newEvent = AtomicSpyLevel(
                            eventType = event.toEventType(),
                            path = path ?: TauPath.EMPTY,
                            itemType = if (path?.toFile()?.map { it.isFile }
                                    ?.getOrNull() == true) ItemType.FILE else ItemType.FOLDER,
                            modificationDate = fileDate1.toTauDate()
                        )

                        println("Spy detected event: ${newEvent.eventType}, ${newEvent.path.value}")
                        emitSpyLevel(newEvent)
                    }
                )

//                fileObserver = changeObserverWith(folderPath)
                //TODO voir légitimité
                fileObserver?.startWatching()
                //TODO une méthode pour décider de quoi faire dans CIA? existe déjà?
                //ici on émet un GlobalScan
                val event = GlobalSpyLevel(folderPath)
                emitSpyLevel(event)
            }

        }.launchIn(scope)

        enabledFlow.onEach { newEnabled ->
            if (newEnabled)
                fileObserver?.startWatching()
            else
                fileObserver?.stopWatching()
        }.launchIn(scope)
    }
}