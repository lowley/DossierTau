package lorry.dossiertau.data.intelligenceService

import io.github.irgaly.kfswatch.KfsDirectoryWatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
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
import lorry.dossiertau.data.intelligenceService.utils2.events.DebouncedTimer
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauDate
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
open class Spy(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    var fileObserver: TauFileObserver = TauFileObserver.of(INACTIVE),
    val fileRepo: IFolderRepo,
    private val scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())
) : ISpy {
    val watcher = KfsDirectoryWatcher(scope)

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
    override val maxTimer = DebouncedTimer(scope)

    /////////////////////////////////
    // gestion des events entrants //
    /////////////////////////////////
    private var _lastSnapshot = MutableStateFlow(Snapshot.EMPTY(observedFolderFlow.value))
    override val lastSnapshotFlow: StateFlow<Snapshot> = _lastSnapshot.asStateFlow()

    override fun setLastSnapshot(newSnapshot: Snapshot) {
        _lastSnapshot.update { newSnapshot }
    }

    ////////////////////////////
    // arrivée d'un évènement //
    ////////////////////////////
    private val ticks = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 64
    )

    @OptIn(ExperimentalAtomicApi::class)
    private val dirty = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    override fun tick() {
        val enabled = enabledFlow.value
        if (enabled) {
            dirty.store(true)
            ticks.tryEmit(Unit)
        }
    }

    override fun computeDiffsBetween(
        sn1: Snapshot,
        sn2: Snapshot
    ): List<ISpyLevel> {

        val createdItems = sn2.entries.minus(sn1.entries.toSet())
        val deletedItems = sn1.entries.minus(sn2.entries.toSet())
        val folderPath = observedFolderFlow.value

        val creationSpyLevels: List<ISpyLevel> = createdItems.map { item ->
            AtomicSpyLevel(
                eventType = AtomicEventType.CREATE,
                path = folderPath.appendToTauPath(item.name),
                itemType = if (item.isDir) ItemType.FOLDER else ItemType.FILE,
                modificationDate = item.lastModified.toTauDate()
            )
        }

        val deletionSpyLevels: List<ISpyLevel> = deletedItems.map { item ->
            AtomicSpyLevel(
                eventType = AtomicEventType.DELETE,
                path = folderPath.appendToTauPath(item.name),
                itemType = if (item.isDir) ItemType.FOLDER else ItemType.FILE,
                modificationDate = item.lastModified.toTauDate()
            )
        }

        return creationSpyLevels + deletionSpyLevels
    }

    ///////////////////////////////////////////////////////////////////////
    // évènements créés par l'espion suite à une opération sur le disque //
    ///////////////////////////////////////////////////////////////////////
    val _spyLevelFlow = MutableSharedFlow<List<ISpyLevel>>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val spyLevelFlow: SharedFlow<List<ISpyLevel>> = _spyLevelFlow.asSharedFlow()

    override fun emitSpyLevel(event: ISpyLevel) {
        scope.launch(dispatcher) {
            _spyLevelFlow.tryEmit(listOf(event))
        }
    }

    override fun emitSpyLevels(events: List<ISpyLevel>) {
        scope.launch(dispatcher) {
            _spyLevelFlow.tryEmit(events)
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
        data class PrevCurr<T>(val prev: T?, val curr: T)

        // 0) Action "snapshot + diffs" qui lit TOUJOURS le folder courant au moment de l'exécution
        suspend fun afterEndOfDelayLatestFolder() {
            println("entrée dans afterEndOfDelayLatestFolder()")
            val currentFolderPath = observedFolderFlow.value

            val newSnapshot = fileRepo.createSnapshotFor(currentFolderPath)
            val diffs = computeDiffsBetween(lastSnapshotFlow.value, newSnapshot)

            if (diffs.isNotEmpty()) emitSpyLevels(diffs)
            setLastSnapshot(newSnapshot)
        }

        // 1) Un seul collect KFS -> tick()
        scope.launch(dispatcher) {
            watcher.onEventFlow.collect {
                tick()
            }
        }

        // 2) Un seul collect ticks -> timers
        scope.launch(dispatcher) {
            ticks.collect {
                minTimer.start(quietWindowMs) {
                    minTimer.cancel()
                    maxTimer.cancel()
                    scope.launch(dispatcher) { afterEndOfDelayLatestFolder() }
                }

                maxTimer.start(maxWaitMs) {
                    minTimer.cancel()
                    maxTimer.cancel()
                    scope.launch(dispatcher) { afterEndOfDelayLatestFolder() }
                }
            }
        }

        // 3) Le flow "folder changé" ne fait plus que : watcher add/remove + snapshot initial + event global
        observedFolderFlow
            .onEach { println("nouvelle valeur de folderFlow: ${it.path}") }
            .filter { it != TauPath.EMPTY }     // ✅ on ignore le “dossier” EMPTY comme cible
            .distinctUntilChanged()
            .runningFold<TauPath, PrevCurr<TauPath>>(
                PrevCurr(
                    prev = TauPath.EMPTY,
                    curr = TauPath.EMPTY
                )
            ) { acc, curr ->
                PrevCurr(prev = acc.curr, curr = curr)
            }
            .drop(1)
            .onEach { (previousFolderPath, currentFolderPath) ->
                println("entrée dans bloc exécution folderFlow: ${currentFolderPath.path}")
                emitSpyLevel(GlobalSpyLevel(path = currentFolderPath))

                if (previousFolderPath != null)
                    scope.launch(dispatcher) {
                        watcher.remove(previousFolderPath.path)
                    }

                scope.launch(dispatcher) {
                    watcher.add(currentFolderPath.path)
                }

                // snapshot initial du folder courant
                println("[SPY ${Thread.currentThread().name}] appel à createSnapshotFor (${currentFolderPath.path})")
                setLastSnapshot(fileRepo.createSnapshotFor(currentFolderPath))
            }
            .launchIn(scope)
    }
}