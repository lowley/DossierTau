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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserver
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicEventType
import lorry.dossiertau.data.intelligenceService.utils.events.AtomicUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalUpdateEvent
import lorry.dossiertau.data.intelligenceService.utils.events.IUpdateEvent
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauPath

import lorry.dossiertau.data.intelligenceService.utils.TauFileObserverInside.INACTIVE
import lorry.dossiertau.data.intelligenceService.utils.events.toEventType
import lorry.dossiertau.support.littleClasses.FolderPath
import lorry.dossiertau.support.littleClasses.toTauDate
import java.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class Spy(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    var fileObserver: TauFileObserver = TauFileObserver.of(INACTIVE)
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

    //////////////////////////
    // inhibiteur de l'init //
    //////////////////////////




    ///////////////////////////////////////////////////////////////////////
    // évènements créés par l'espion suite à une opération sur le disque //
    ///////////////////////////////////////////////////////////////////////
    val _updateEventFlow = MutableSharedFlow<IUpdateEvent>()

    override val updateEventFlow: SharedFlow<IUpdateEvent> = _updateEventFlow.asSharedFlow()

    override fun emitIncomingEvent(event: IUpdateEvent) {
        scope.launch(dispatcher) {
            _updateEventFlow.emit(event)
        }
    }

    override fun emitFake_CREATEITEM(
        itemToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate
    ) {
        val fakeEvent = AtomicUpdateEvent(
            eventType = AtomicEventType.CREATE,
            path = itemToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
        )

        emitIncomingEvent(fakeEvent)
    }

    override fun emitFake_DELETEITEM(
        itemToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate
    ) {
        val fakeEvent = AtomicUpdateEvent(
            eventType = AtomicEventType.DELETE,
            path = itemToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
        )

        emitIncomingEvent(fakeEvent)
    }

    override fun emitFake_MODIFYITEM(
        itemToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate
    ) {
        val fakeEvent = AtomicUpdateEvent(
            eventType = AtomicEventType.MODIFY,
            path = itemToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
        )

        emitIncomingEvent(fakeEvent)
    }

    override fun emitFake_MOVEDFROM(
        itemToEmit: TauPath,
        itemType: ItemType,
        modificationDate: TauDate
    ) {
        val fakeEvent = AtomicUpdateEvent(
            eventType = AtomicEventType.MOVED_FROM,
            path = itemToEmit,
            itemType = itemType,
            modificationDate = modificationDate,
        )

        emitIncomingEvent(fakeEvent)
    }

    suspend fun doOnEvent(atomicUpdateEvent: AtomicUpdateEvent) {
        emitIncomingEvent(atomicUpdateEvent)
    }

    init {

        //////////////
        // réglages //
        //////////////
        observedFolderFlow.onEach { folderPath ->
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
                        val newEvent = AtomicUpdateEvent(
                            eventType = event.toEventType(),
                            path = path ?: TauPath.EMPTY,
                            itemType = if (path?.toFile()?.map { it.isFile }
                                    ?.getOrNull() == true) ItemType.FILE else ItemType.FOLDER,
                           modificationDate = fileDate1.toTauDate()
                        )

                        println("Spy detected event: ${newEvent.eventType}, ${newEvent.path.value}")
                        emitIncomingEvent(newEvent)
                    }
                )

//                fileObserver = changeObserverWith(folderPath)
                //TODO voir légitimité
                fileObserver?.startWatching()
                //TODO une méthode pour décider de quoi faire dans CIA? existe déjà?
                //ici on émet un GlobalScan
                val event = GlobalUpdateEvent(folderPath)
                emitIncomingEvent(event)
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