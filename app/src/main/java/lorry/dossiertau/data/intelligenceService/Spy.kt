package lorry.dossiertau.data.intelligenceService

import androidx.compose.animation.core.snap
import io.github.irgaly.kfswatch.KfsDirectoryWatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
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
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalItem
import lorry.dossiertau.data.intelligenceService.utils2.events.DebouncedTimer
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauDate
import lorry.dossiertau.usecases.applicationFavorites.AppliFavos
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.koin.java.KoinJavaComponent.inject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.ExperimentalTime
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalTime::class)
open class Spy(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    var fileObserver: TauFileObserver = TauFileObserver.of(INACTIVE),
    val fileRepo: IFolderRepo,
    private val scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())
) : ISpy {
    val watcher = KfsDirectoryWatcher(scope)
    val appliFavos: AppliFavos by inject(AppliFavos::class.java)
    private val instanceId = System.identityHashCode(this).toString(16).uppercase().take(5)

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

    // Le canal pour recevoir les demandes de snapshot
    // Capacity = UNLIMITED pour ne rater aucune modif disque
    private val commandChannel = Channel<Unit>(Channel.UNLIMITED)
    override val snapshotsAtomic =
        AtomicReference<Map<TauPath, Snapshot>>(mapOf(TauPath.EMPTY to Snapshot.EMPTY(TauPath.EMPTY)))
    private val _storedSnapshots = MutableStateFlow(snapshotsAtomic.get())
    override val storedSnapshotsFlow: StateFlow<Map<TauPath, Snapshot>> =
        _storedSnapshots.asStateFlow()

    override fun setStoredSnapshot(newSnapshot: Snapshot) {
        println("from setLastSnapshot: ${newSnapshot.entries.size}")
        val oldAtomic = snapshotsAtomic.get()
        snapshotsAtomic.set(oldAtomic.plus(newSnapshot.folderPath to newSnapshot))
        _storedSnapshots.value = snapshotsAtomic.get()
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

        val creationOrModificationSpyLevels: List<ISpyLevel> = createdItems.map { item ->
            val sameFileId = sn1.entries.firstOrNull { it.fileId == item.fileId }

            if (sameFileId != null) {
                //renommage
                AtomicSpyLevel(
                    eventType = AtomicEventType.MODIFY,
                    path = folderPath.appendToTauPath(item.name),
                    itemType = if (item.isDir) ItemType.FOLDER else ItemType.FILE,
                    modificationDate = item.lastModified.toTauDate(),
                    itemId = item.fileId,
                    picture = item.picture,
                    memo = item.memo
                )
            } else
            //création
                AtomicSpyLevel(
                    eventType = AtomicEventType.CREATE,
                    path = folderPath.appendToTauPath(item.name),
                    itemType = if (item.isDir) ItemType.FOLDER else ItemType.FILE,
                    modificationDate = item.lastModified.toTauDate(),
                    itemId = item.fileId,
                    picture = item.picture,
                    memo = item.memo
                )
        }

        val deletionSpyLevels: List<ISpyLevel> = deletedItems.mapNotNull { item ->
            val sameFileId = creationOrModificationSpyLevels.firstOrNull {
                (it as AtomicSpyLevel).itemId == item.fileId
            }

            //si sameFileId != null, le fichier existant dans creationOrModificationSpyLevels
            //c'est qu'il est renommé donc déjà traité par MODIFY
            if (sameFileId == null)
                AtomicSpyLevel(
                    eventType = AtomicEventType.DELETE,
                    path = folderPath.appendToTauPath(item.name),
                    itemType = if (item.isDir) ItemType.FOLDER else ItemType.FILE,
                    modificationDate = item.lastModified.toTauDate(),
                    itemId = item.fileId,
                    picture = item.picture,
                    memo = item.memo
                )
            else null
        }

        return creationOrModificationSpyLevels + deletionSpyLevels
    }

    private fun makeGlobalSpyLevelFrom(sn2: Snapshot): GlobalSpyLevel = GlobalSpyLevel(
        path = sn2.folderPath,
        items = sn2.entries.map { snelem ->
            GlobalItem(
                path = sn2.folderPath.appendToTauPath(snelem.name),
                itemType = if (snelem.isDir) ItemType.FOLDER else ItemType.FILE,
                modificationDate = snelem.lastModified.toTauDate(),
                itemId = snelem.fileId,
                picture = snelem.picture,
                memo = snelem.memo
            )
        },
    )

    ///////////////////////////////////////////////////////////////////////
    // évènements créés par l'espion suite à une opération sur le disque //
    ///////////////////////////////////////////////////////////////////////
    val _spyLevelFlow = MutableSharedFlow<List<ISpyLevel>>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val spyLevelFlow: SharedFlow<List<ISpyLevel>> = _spyLevelFlow.asSharedFlow()

    // Les événements passent d'abord par une file persistante en mémoire.
    // Cela évite de perdre le snapshot initial si CIA n'a pas encore commencé
    // à collecter spyLevelFlow au moment où le premier dossier est sélectionné.
    private val spyEventChannel = Channel<List<ISpyLevel>>(Channel.UNLIMITED)

    override fun emitSpyLevel(event: ISpyLevel) {
        spyEventChannel.trySend(listOf(event))
    }

    override fun emitSpyLevels(events: List<ISpyLevel>) {
        if (events.isNotEmpty()) {
            spyEventChannel.trySend(events)
        }
    }

    override fun emitFake_CREATEITEM(
        itemToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate,
        fileId: FileId
    ) {
        val fakeEvent = AtomicSpyLevel(
            eventType = AtomicEventType.CREATE,
            path = itemToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
            itemId = fileId,
            picture = TauPicture.NONE,
            memo = "truc"
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
            itemId = FileId.fileIdOf(5L, 8L),
            picture = TauPicture.NONE,
            memo = "truc"
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
            itemId = FileId.fileIdOf(5L, 8L),
            picture = TauPicture.NONE,
            memo = "truc"
        )

        emitSpyLevel(fakeEvent)
    }

    suspend fun doOnEvent(atomicUpdateEvent: AtomicSpyLevel) {
        emitSpyLevel(atomicUpdateEvent)
    }

    ///////////////////////
    // compteur de diffs //
    ///////////////////////
    val _diffNumberFlow = MutableStateFlow(0)
    override val diffNumberFlow: StateFlow<Int> = _diffNumberFlow.asStateFlow()

    override fun incrementDiffNumber() {
        _diffNumberFlow.update { it + 1 }
    }

    override fun resetDiffNumber() {
        _diffNumberFlow.update { 0 }
    }

    val diffMaxBeforeGlobal = 10

    // 0) Action "snapshot + diffs" qui lit TOUJOURS le folder courant au moment de l'exécution
    suspend fun executeSnapshotLogic() {
        println("[SPY $instanceId] entrée dans afterEndOfDelayLatestFolder()")
        val currentFolderPath = observedFolderFlow.value

        val newSnapshot = fileRepo.createSnapshotFor(currentFolderPath)
        val diffNumber = diffNumberFlow.value

        if (diffNumber > diffMaxBeforeGlobal) {

            resetDiffNumber()
            emitSpyLevel(makeGlobalSpyLevelFrom(newSnapshot))
            return
        }

        val oldSnapshot =
            snapshotsAtomic.get().get(currentFolderPath) ?: Snapshot(currentFolderPath, emptyMap())
//        println("from afterEndOfDelayLatestFolder: lastSnapshot[SN ${lastSnapshotFlow.value.instanceId}](${lastSnapshotFlow.value.entries.size})")
        println("from afterEndOfDelayLatestFolder: oldSnapshot[SN ${oldSnapshot.instanceId}](${oldSnapshot.entries.size})")
        println("from afterEndOfDelayLatestFolder: newSnapshot(${newSnapshot.entries.size})")
        val diffs = computeDiffsBetween(oldSnapshot, newSnapshot)

        println("from afterEndOfDelayLatestFolder: setLastSnapshot[SN ${newSnapshot.instanceId}](${newSnapshot.entries.size})")
        setStoredSnapshot(newSnapshot)
        if (diffs.isNotEmpty()) emitSpyLevels(diffs)
    }

    init {
        // Un seul convoyeur pour les événements Spy.
        // Il conserve l'ordre et attend qu'un consommateur (CIA) soit réellement
        // abonné avant d'émettre. Aucun événement de démarrage n'est donc perdu.
        scope.launch(dispatcher) {
            for (events in spyEventChannel) {
                _spyLevelFlow.subscriptionCount.first { subscriberCount ->
                    subscriberCount > 0
                }
                _spyLevelFlow.emit(events)
            }
        }

        // Le SEUL endroit où l'on traite les snapshots
        // Cette coroutine tourne en boucle et traite les messages un par un
        scope.launch(dispatcher) {
            for (command in commandChannel) {
                executeSnapshotLogic()
            }
        }

        //////////////
        // réglages //
        //////////////
        data class PrevCurr<T>(val prev: T?, val curr: T)

        suspend fun afterEndOfDelayLatestFolder() {
            println("[$instanceId] Signal de mise à jour envoyé au Channel")
            commandChannel.send(Unit)
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

        // 3) Le flow "folder changé" ne fait plus que: watcher add/remove + snapshot initial + event global
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
                println("[SPY $instanceId] entrée dans bloc exécution folderFlow: ${currentFolderPath.path}")

                if (previousFolderPath?.value?.isRight() == true)
                    scope.launch(dispatcher) {
                        watcher.remove(previousFolderPath.path)
                    }

                scope.launch(dispatcher) {
                    watcher.add(currentFolderPath.path)
                }

                val favoris = appliFavos.appliFavorites.value
                val sn = snapshotsAtomic.get()[currentFolderPath]
                if (currentFolderPath.path in favoris.map { it.fullPath.path } &&
                    sn != null) {

                    emitSpyLevel(makeGlobalSpyLevelFrom(sn))
                } else {

                    // snapshot initial du folder courant
                    println("[SPY ${Thread.currentThread().name}] appel à createSnapshotFor (${currentFolderPath.path})")
                    val initialSnapshot = fileRepo.createSnapshotFor(currentFolderPath)
                    println("from observedFolderFlow: setLastSnapshot[SN ${initialSnapshot.instanceId}](${initialSnapshot.entries.size})")
                    setStoredSnapshot(initialSnapshot)
                    emitSpyLevel(makeGlobalSpyLevelFrom(initialSnapshot))
                }

            }
            .launchIn(scope)

        //////////////////////////////////////////////////////////
        // les favoris sont à tout moment suivis par le watcher //
        //////////////////////////////////////////////////////////

        //on ajoute tous les favoris au watcher au début de l'app
        val currentFavorites = appliFavos.appliFavorites
        scope.launch(dispatcher) {
            currentFavorites.first().forEach { favorite ->
                watcher.add(favorite.fullPath.path)
            }
        }

        //suivi des favoris ajoutés pendant la vie de l'app
        scope.launch(dispatcher) {
            appliFavos.addedFavorite.filterNotNull().collect { favorite ->
                watcher.add(favorite.fullPath.path)
            }
        }

        //suivi des favoris supprimés pendant la vie de l'app
        scope.launch(dispatcher) {
            appliFavos.removedFavorite.filterNotNull().collect { favorite ->
                watcher.remove(favorite.fullPath.path)
            }
        }
    }
}